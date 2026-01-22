// SPDX-License-Identifier: MIT
pragma solidity ^0.8.22;

import "@openzeppelin/contracts-upgradeable/token/ERC20/ERC20Upgradeable.sol";
import "@openzeppelin/contracts-upgradeable/token/ERC20/extensions/ERC20BurnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/token/ERC20/extensions/ERC20PausableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";

/**
 * @title StrategizToken (STRAT)
 * @dev ERC-20 utility token for the Strategiz platform
 *
 * TOKENOMICS:
 * - Total Supply: 1,000,000,000 (1 billion) STRAT
 * - Decimals: 18
 * - Exchange Rate: $1 USD = 100 STRAT
 *
 * DISTRIBUTION:
 * - Platform Operations: 50% (500,000,000 STRAT) - Purchases, rewards, referrals
 * - Liquidity:           20% (200,000,000 STRAT) - DEX when trading enabled
 * - Development:         15% (150,000,000 STRAT) - 2-year linear vesting
 * - Marketing:           10% (100,000,000 STRAT) - User acquisition
 * - Team:                 5% ( 50,000,000 STRAT) - 2-year vesting, 1-year cliff
 *
 * PHASES:
 * - Phase 1 (Utility): transfersRestricted = true
 *   → Only platform can move tokens (tips, subscriptions, purchases)
 *   → Users see balance in app, spend through platform actions
 *
 * - Phase 2 (Trading): transfersRestricted = false
 *   → Users can transfer freely, trade on DEX, bridge cross-chain
 *   → Same token, same contract, same balances
 *
 * FEATURES:
 * - Transfer restrictions (utility phase)
 * - Anti-whale protection (trading phase)
 * - Vesting schedules for team/dev
 * - Bridge-ready for cross-chain
 * - Upgradeable via UUPS proxy
 */
contract StrategizToken is
    Initializable,
    ERC20Upgradeable,
    ERC20BurnableUpgradeable,
    ERC20PausableUpgradeable,
    AccessControlUpgradeable,
    UUPSUpgradeable
{
    // ============ ROLES ============
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant TRANSFER_ROLE = keccak256("TRANSFER_ROLE");
    bytes32 public constant BRIDGE_ROLE = keccak256("BRIDGE_ROLE");
    bytes32 public constant UPGRADER_ROLE = keccak256("UPGRADER_ROLE");

    // ============ CONSTANTS ============
    uint256 public constant TOTAL_SUPPLY = 1_000_000_000 * 10**18; // 1 billion
    uint256 public constant PLATFORM_ALLOCATION = 500_000_000 * 10**18;     // 50%
    uint256 public constant LIQUIDITY_ALLOCATION = 200_000_000 * 10**18;    // 20%
    uint256 public constant DEVELOPMENT_ALLOCATION = 150_000_000 * 10**18;  // 15%
    uint256 public constant MARKETING_ALLOCATION = 100_000_000 * 10**18;    // 10%
    uint256 public constant TEAM_ALLOCATION = 50_000_000 * 10**18;          // 5%

    // ============ STATE VARIABLES ============

    /// @notice Phase 1 = true (utility), Phase 2 = false (trading)
    bool public transfersRestricted;

    // Anti-whale settings (for trading phase)
    uint256 public maxTransferAmount;      // Max tokens per transfer (0 = disabled)
    uint256 public transferTaxBps;         // Tax in basis points (100 = 1%, max 1000 = 10%)
    address public treasuryWallet;
    mapping(address => bool) public isExemptFromTax;
    mapping(address => bool) public isExemptFromLimit;

    // Vesting
    struct VestingSchedule {
        uint256 totalAmount;
        uint256 releasedAmount;
        uint256 startTime;
        uint256 cliffDuration;
        uint256 vestingDuration;
    }
    mapping(address => VestingSchedule) public vestingSchedules;

    // Allocation wallet addresses
    address public platformWallet;
    address public liquidityWallet;
    address public developmentWallet;
    address public marketingWallet;
    address public teamWallet;

    // Flag for platform-initiated transfers (allows transfers on behalf of users)
    bool private _platformTransferInProgress;

    // ============ EVENTS ============
    event PhaseChanged(bool transfersRestricted, string phase);
    event TokensMinted(address indexed to, uint256 amount, string orderId);
    event TokensSpent(address indexed from, uint256 amount, string reason);
    event TokensTipped(address indexed from, address indexed to, uint256 amount);
    event VestingReleased(address indexed beneficiary, uint256 amount);
    event BridgeMint(address indexed to, uint256 amount, string sourceChain, string txHash);
    event BridgeBurn(address indexed from, uint256 amount, string targetChain);
    event TaxCollected(address indexed from, uint256 amount);

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    /**
     * @dev Initializes the token with full tokenomics distribution
     * @param admin Default admin address
     * @param _platformWallet Platform operations wallet (50%)
     * @param _liquidityWallet Liquidity wallet (20%)
     * @param _developmentWallet Development wallet (15%)
     * @param _marketingWallet Marketing wallet (10%)
     * @param _teamWallet Team wallet (5%)
     * @param _treasuryWallet Treasury for tax collection
     */
    function initialize(
        address admin,
        address _platformWallet,
        address _liquidityWallet,
        address _developmentWallet,
        address _marketingWallet,
        address _teamWallet,
        address _treasuryWallet
    ) public initializer {
        __ERC20_init("Strategiz Token", "STRAT");
        __ERC20Burnable_init();
        __ERC20Pausable_init();
        __AccessControl_init();
        __UUPSUpgradeable_init();

        // Store wallet addresses
        platformWallet = _platformWallet;
        liquidityWallet = _liquidityWallet;
        developmentWallet = _developmentWallet;
        marketingWallet = _marketingWallet;
        teamWallet = _teamWallet;
        treasuryWallet = _treasuryWallet;

        // Grant roles to admin
        _grantRole(DEFAULT_ADMIN_ROLE, admin);
        _grantRole(PAUSER_ROLE, admin);
        _grantRole(UPGRADER_ROLE, admin);

        // Platform wallet gets minter and transfer roles
        _grantRole(MINTER_ROLE, _platformWallet);
        _grantRole(TRANSFER_ROLE, _platformWallet);
        _grantRole(TRANSFER_ROLE, _treasuryWallet);

        // Exempt system wallets from tax/limits
        _setExemptions(_platformWallet, true, true);
        _setExemptions(_liquidityWallet, true, true);
        _setExemptions(_developmentWallet, true, true);
        _setExemptions(_marketingWallet, true, true);
        _setExemptions(_teamWallet, true, true);
        _setExemptions(_treasuryWallet, true, true);

        // Mint total supply to allocation wallets
        _mint(_platformWallet, PLATFORM_ALLOCATION);
        _mint(_liquidityWallet, LIQUIDITY_ALLOCATION);
        _mint(_developmentWallet, DEVELOPMENT_ALLOCATION);
        _mint(_marketingWallet, MARKETING_ALLOCATION);
        _mint(_teamWallet, TEAM_ALLOCATION);

        // Set up vesting schedules
        _createVestingSchedule(_teamWallet, TEAM_ALLOCATION, 365 days, 730 days);        // 1-year cliff, 2-year total
        _createVestingSchedule(_developmentWallet, DEVELOPMENT_ALLOCATION, 0, 730 days); // No cliff, 2-year linear

        // Start in UTILITY PHASE (transfers restricted)
        transfersRestricted = true;

        // Anti-whale disabled by default (enable in trading phase)
        maxTransferAmount = 0;
        transferTaxBps = 0;

        emit PhaseChanged(true, "utility");
    }

    // ============ CORE TOKEN FUNCTIONS ============

    function decimals() public pure override returns (uint8) {
        return 18;
    }

    /**
     * @dev Transfer hook - enforces restrictions and tax
     */
    function _update(
        address from,
        address to,
        uint256 value
    ) internal override(ERC20Upgradeable, ERC20PausableUpgradeable) {
        // Skip checks for mint/burn
        if (from != address(0) && to != address(0)) {

            // UTILITY PHASE: Only platform can transfer
            if (transfersRestricted) {
                require(
                    _platformTransferInProgress ||
                    hasRole(TRANSFER_ROLE, from) ||
                    hasRole(TRANSFER_ROLE, to),
                    "STRAT: utility phase - transfers restricted"
                );
            }

            // TRADING PHASE: Apply anti-whale and tax
            else {
                // Max transfer limit
                if (maxTransferAmount > 0 && !isExemptFromLimit[from] && !isExemptFromLimit[to]) {
                    require(value <= maxTransferAmount, "STRAT: exceeds max transfer");
                }

                // Transfer tax
                if (transferTaxBps > 0 && !isExemptFromTax[from] && !isExemptFromTax[to]) {
                    uint256 tax = (value * transferTaxBps) / 10000;
                    if (tax > 0) {
                        super._update(from, treasuryWallet, tax);
                        emit TaxCollected(from, tax);
                        value -= tax;
                    }
                }
            }
        }

        super._update(from, to, value);
    }

    // ============ PLATFORM FUNCTIONS (Phase 1 & 2) ============

    /**
     * @dev Mint tokens when user purchases a STRAT pack
     * @param buyer User receiving tokens
     * @param amount Amount in wei (with 18 decimals)
     * @param orderId Stripe session ID or order reference
     */
    function mintForPurchase(
        address buyer,
        uint256 amount,
        string calldata orderId
    ) external onlyRole(MINTER_ROLE) {
        _transfer(platformWallet, buyer, amount);
        emit TokensMinted(buyer, amount, orderId);
    }

    /**
     * @dev Process a tip from one user to another
     * @param from Tipper address
     * @param to Creator address
     * @param amount Tip amount
     */
    function processTip(
        address from,
        address to,
        uint256 amount
    ) external onlyRole(TRANSFER_ROLE) {
        _platformTransferInProgress = true;
        _transfer(from, to, amount);
        _platformTransferInProgress = false;
        emit TokensTipped(from, to, amount);
    }

    /**
     * @dev Burn tokens for platform spend (subscriptions, etc.)
     * @param amount Amount to burn
     * @param reason Description of spend
     */
    function spend(uint256 amount, string calldata reason) external {
        _burn(_msgSender(), amount);
        emit TokensSpent(_msgSender(), amount, reason);
    }

    /**
     * @dev Platform burns tokens on behalf of user (for subscriptions)
     */
    function spendFrom(
        address from,
        uint256 amount,
        string calldata reason
    ) external onlyRole(TRANSFER_ROLE) {
        _burn(from, amount);
        emit TokensSpent(from, amount, reason);
    }

    // ============ VESTING FUNCTIONS ============

    function _createVestingSchedule(
        address beneficiary,
        uint256 totalAmount,
        uint256 cliffDuration,
        uint256 vestingDuration
    ) internal {
        vestingSchedules[beneficiary] = VestingSchedule({
            totalAmount: totalAmount,
            releasedAmount: 0,
            startTime: block.timestamp,
            cliffDuration: cliffDuration,
            vestingDuration: vestingDuration
        });
    }

    /**
     * @dev Get vested (unlocked) amount for a beneficiary
     */
    function getVestedAmount(address beneficiary) public view returns (uint256) {
        VestingSchedule storage schedule = vestingSchedules[beneficiary];
        if (schedule.totalAmount == 0) return 0;

        uint256 elapsed = block.timestamp - schedule.startTime;

        // Before cliff: nothing vested
        if (elapsed < schedule.cliffDuration) {
            return 0;
        }

        // After full vesting: everything vested
        if (elapsed >= schedule.vestingDuration) {
            return schedule.totalAmount;
        }

        // Linear vesting
        return (schedule.totalAmount * elapsed) / schedule.vestingDuration;
    }

    /**
     * @dev Get releasable (vested but not yet released) amount
     */
    function getReleasableAmount(address beneficiary) public view returns (uint256) {
        return getVestedAmount(beneficiary) - vestingSchedules[beneficiary].releasedAmount;
    }

    /**
     * @dev Release vested tokens (callable by beneficiary)
     */
    function releaseVested() external {
        uint256 releasable = getReleasableAmount(_msgSender());
        require(releasable > 0, "STRAT: nothing to release");

        vestingSchedules[_msgSender()].releasedAmount += releasable;
        emit VestingReleased(_msgSender(), releasable);
        // Note: Tokens are already in beneficiary wallet, vesting just tracks unlock
    }

    // ============ BRIDGE FUNCTIONS (Phase 2) ============

    /**
     * @dev Mint tokens from bridge (cross-chain transfer in)
     */
    function bridgeMint(
        address to,
        uint256 amount,
        string calldata sourceChain,
        string calldata txHash
    ) external onlyRole(BRIDGE_ROLE) {
        _mint(to, amount);
        emit BridgeMint(to, amount, sourceChain, txHash);
    }

    /**
     * @dev Burn tokens for bridge (cross-chain transfer out)
     */
    function bridgeBurn(uint256 amount, string calldata targetChain) external {
        _burn(_msgSender(), amount);
        emit BridgeBurn(_msgSender(), amount, targetChain);
    }

    // ============ ADMIN FUNCTIONS ============

    /**
     * @dev Switch from utility phase to trading phase
     * Call this when ready to enable public trading
     */
    function enableTrading() external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(transfersRestricted, "STRAT: trading already enabled");
        transfersRestricted = false;
        emit PhaseChanged(false, "trading");
    }

    /**
     * @dev Emergency: re-enable restrictions (use with caution)
     */
    function disableTrading() external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(!transfersRestricted, "STRAT: trading already disabled");
        transfersRestricted = true;
        emit PhaseChanged(true, "utility");
    }

    /**
     * @dev Configure anti-whale settings (for trading phase)
     * @param _maxTransfer Max tokens per transfer (0 = disabled)
     * @param _taxBps Tax in basis points (100 = 1%, max 1000 = 10%)
     */
    function setAntiWhale(
        uint256 _maxTransfer,
        uint256 _taxBps
    ) external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(_taxBps <= 1000, "STRAT: max tax is 10%");
        maxTransferAmount = _maxTransfer;
        transferTaxBps = _taxBps;
    }

    /**
     * @dev Set exemptions for an address
     */
    function setExemptions(
        address account,
        bool taxExempt,
        bool limitExempt
    ) external onlyRole(DEFAULT_ADMIN_ROLE) {
        _setExemptions(account, taxExempt, limitExempt);
    }

    function _setExemptions(address account, bool taxExempt, bool limitExempt) internal {
        isExemptFromTax[account] = taxExempt;
        isExemptFromLimit[account] = limitExempt;
    }

    /**
     * @dev Grant transfer role to platform backend wallet
     */
    function grantTransferRole(address account) external onlyRole(DEFAULT_ADMIN_ROLE) {
        _grantRole(TRANSFER_ROLE, account);
    }

    /**
     * @dev Grant bridge role to bridge contract
     */
    function grantBridgeRole(address account) external onlyRole(DEFAULT_ADMIN_ROLE) {
        _grantRole(BRIDGE_ROLE, account);
    }

    /**
     * @dev Update treasury wallet address
     */
    function setTreasuryWallet(address _treasury) external onlyRole(DEFAULT_ADMIN_ROLE) {
        treasuryWallet = _treasury;
    }

    function pause() external onlyRole(PAUSER_ROLE) {
        _pause();
    }

    function unpause() external onlyRole(PAUSER_ROLE) {
        _unpause();
    }

    function _authorizeUpgrade(address newImplementation) internal override onlyRole(UPGRADER_ROLE) {}
}
