import { expect } from "chai";
import { ethers, upgrades } from "hardhat";
import { StrategizToken } from "../typechain-types";
import { SignerWithAddress } from "@nomicfoundation/hardhat-ethers/signers";

describe("StrategizToken", function () {
  let token: StrategizToken;
  let admin: SignerWithAddress;
  let platformWallet: SignerWithAddress;
  let liquidityWallet: SignerWithAddress;
  let devWallet: SignerWithAddress;
  let marketingWallet: SignerWithAddress;
  let teamWallet: SignerWithAddress;
  let treasuryWallet: SignerWithAddress;
  let user1: SignerWithAddress;
  let user2: SignerWithAddress;

  const TOTAL_SUPPLY = ethers.parseEther("1000000000"); // 1 billion
  const PLATFORM_ALLOCATION = ethers.parseEther("500000000"); // 50%
  const LIQUIDITY_ALLOCATION = ethers.parseEther("200000000"); // 20%
  const DEV_ALLOCATION = ethers.parseEther("150000000"); // 15%
  const MARKETING_ALLOCATION = ethers.parseEther("100000000"); // 10%
  const TEAM_ALLOCATION = ethers.parseEther("50000000"); // 5%

  beforeEach(async function () {
    [admin, platformWallet, liquidityWallet, devWallet, marketingWallet, teamWallet, treasuryWallet, user1, user2] =
      await ethers.getSigners();

    const StrategizToken = await ethers.getContractFactory("StrategizToken");
    token = (await upgrades.deployProxy(
      StrategizToken,
      [
        admin.address,
        platformWallet.address,
        liquidityWallet.address,
        devWallet.address,
        marketingWallet.address,
        teamWallet.address,
        treasuryWallet.address,
      ],
      { initializer: "initialize", kind: "uups" }
    )) as unknown as StrategizToken;

    await token.waitForDeployment();
  });

  describe("Deployment & Tokenomics", function () {
    it("Should have correct name and symbol", async function () {
      expect(await token.name()).to.equal("Strategiz Token");
      expect(await token.symbol()).to.equal("STRAT");
    });

    it("Should have 18 decimals", async function () {
      expect(await token.decimals()).to.equal(18);
    });

    it("Should mint total supply of 1 billion", async function () {
      expect(await token.totalSupply()).to.equal(TOTAL_SUPPLY);
    });

    it("Should distribute tokens correctly", async function () {
      expect(await token.balanceOf(platformWallet.address)).to.equal(PLATFORM_ALLOCATION);
      expect(await token.balanceOf(liquidityWallet.address)).to.equal(LIQUIDITY_ALLOCATION);
      expect(await token.balanceOf(devWallet.address)).to.equal(DEV_ALLOCATION);
      expect(await token.balanceOf(marketingWallet.address)).to.equal(MARKETING_ALLOCATION);
      expect(await token.balanceOf(teamWallet.address)).to.equal(TEAM_ALLOCATION);
    });

    it("Should start in utility phase (transfers restricted)", async function () {
      expect(await token.transfersRestricted()).to.equal(true);
    });
  });

  describe("Utility Phase (Transfers Restricted)", function () {
    it("Should block user-to-user transfers", async function () {
      // First, give user1 some tokens via platform
      await token.connect(platformWallet).mintForPurchase(user1.address, ethers.parseEther("1000"), "order_123");

      // User trying to transfer should fail
      await expect(
        token.connect(user1).transfer(user2.address, ethers.parseEther("100"))
      ).to.be.revertedWith("STRAT: utility phase - transfers restricted");
    });

    it("Should allow platform to transfer tokens", async function () {
      await token.connect(platformWallet).mintForPurchase(user1.address, ethers.parseEther("1000"), "order_123");
      expect(await token.balanceOf(user1.address)).to.equal(ethers.parseEther("1000"));
    });

    it("Should allow platform to process tips", async function () {
      // Give user1 tokens
      await token.connect(platformWallet).mintForPurchase(user1.address, ethers.parseEther("1000"), "order_123");

      // Process tip from user1 to user2
      await token.connect(platformWallet).processTip(user1.address, user2.address, ethers.parseEther("100"));

      expect(await token.balanceOf(user1.address)).to.equal(ethers.parseEther("900"));
      expect(await token.balanceOf(user2.address)).to.equal(ethers.parseEther("100"));
    });

    it("Should emit TokensTipped event", async function () {
      await token.connect(platformWallet).mintForPurchase(user1.address, ethers.parseEther("1000"), "order_123");

      await expect(token.connect(platformWallet).processTip(user1.address, user2.address, ethers.parseEther("100")))
        .to.emit(token, "TokensTipped")
        .withArgs(user1.address, user2.address, ethers.parseEther("100"));
    });
  });

  describe("Trading Phase", function () {
    beforeEach(async function () {
      // Enable trading
      await token.connect(admin).enableTrading();
    });

    it("Should allow user-to-user transfers when trading enabled", async function () {
      await token.connect(platformWallet).mintForPurchase(user1.address, ethers.parseEther("1000"), "order_123");

      await token.connect(user1).transfer(user2.address, ethers.parseEther("100"));
      expect(await token.balanceOf(user2.address)).to.equal(ethers.parseEther("100"));
    });

    it("Should emit PhaseChanged event", async function () {
      // Reset to utility phase first
      await token.connect(admin).disableTrading();

      await expect(token.connect(admin).enableTrading())
        .to.emit(token, "PhaseChanged")
        .withArgs(false, "trading");
    });
  });

  describe("Anti-Whale (Trading Phase)", function () {
    beforeEach(async function () {
      await token.connect(admin).enableTrading();
      await token.connect(platformWallet).mintForPurchase(user1.address, ethers.parseEther("100000"), "order_123");
    });

    it("Should enforce max transfer limit", async function () {
      // Set max transfer to 1000 STRAT
      await token.connect(admin).setAntiWhale(ethers.parseEther("1000"), 0);

      await expect(
        token.connect(user1).transfer(user2.address, ethers.parseEther("5000"))
      ).to.be.revertedWith("STRAT: exceeds max transfer");

      // But 1000 should work
      await token.connect(user1).transfer(user2.address, ethers.parseEther("1000"));
      expect(await token.balanceOf(user2.address)).to.equal(ethers.parseEther("1000"));
    });

    it("Should collect transfer tax", async function () {
      // Set 2% tax (200 bps)
      await token.connect(admin).setAntiWhale(0, 200);

      const transferAmount = ethers.parseEther("1000");
      const expectedTax = ethers.parseEther("20"); // 2%
      const expectedReceived = ethers.parseEther("980");

      await token.connect(user1).transfer(user2.address, transferAmount);

      expect(await token.balanceOf(user2.address)).to.equal(expectedReceived);
      expect(await token.balanceOf(treasuryWallet.address)).to.equal(expectedTax);
    });

    it("Should exempt addresses from tax", async function () {
      await token.connect(admin).setAntiWhale(0, 200); // 2% tax
      await token.connect(admin).setExemptions(user1.address, true, true);

      await token.connect(user1).transfer(user2.address, ethers.parseEther("1000"));

      // No tax taken
      expect(await token.balanceOf(user2.address)).to.equal(ethers.parseEther("1000"));
    });

    it("Should reject tax higher than 10%", async function () {
      await expect(
        token.connect(admin).setAntiWhale(0, 1001) // 10.01%
      ).to.be.revertedWith("STRAT: max tax is 10%");
    });
  });

  describe("Platform Functions", function () {
    it("Should mint tokens for purchase from platform wallet", async function () {
      const amount = ethers.parseEther("500");

      await expect(token.connect(platformWallet).mintForPurchase(user1.address, amount, "stripe_session_123"))
        .to.emit(token, "TokensMinted")
        .withArgs(user1.address, amount, "stripe_session_123");

      expect(await token.balanceOf(user1.address)).to.equal(amount);
    });

    it("Should allow users to spend tokens", async function () {
      await token.connect(platformWallet).mintForPurchase(user1.address, ethers.parseEther("1000"), "order_123");

      await expect(token.connect(user1).spend(ethers.parseEther("100"), "subscription:owner123"))
        .to.emit(token, "TokensSpent")
        .withArgs(user1.address, ethers.parseEther("100"), "subscription:owner123");

      expect(await token.balanceOf(user1.address)).to.equal(ethers.parseEther("900"));
    });

    it("Should allow platform to spend on behalf of user", async function () {
      await token.connect(platformWallet).mintForPurchase(user1.address, ethers.parseEther("1000"), "order_123");

      await token.connect(platformWallet).spendFrom(user1.address, ethers.parseEther("100"), "subscription:owner456");

      expect(await token.balanceOf(user1.address)).to.equal(ethers.parseEther("900"));
    });
  });

  describe("Vesting", function () {
    it("Should have vesting schedule for team wallet", async function () {
      const schedule = await token.vestingSchedules(teamWallet.address);
      expect(schedule.totalAmount).to.equal(TEAM_ALLOCATION);
      expect(schedule.cliffDuration).to.equal(365 * 24 * 60 * 60); // 1 year
      expect(schedule.vestingDuration).to.equal(730 * 24 * 60 * 60); // 2 years
    });

    it("Should return 0 vested before cliff", async function () {
      const vested = await token.getVestedAmount(teamWallet.address);
      expect(vested).to.equal(0);
    });

    it("Should have vesting schedule for dev wallet (no cliff)", async function () {
      const schedule = await token.vestingSchedules(devWallet.address);
      expect(schedule.totalAmount).to.equal(DEV_ALLOCATION);
      expect(schedule.cliffDuration).to.equal(0);
    });
  });

  describe("Bridge Functions", function () {
    it("Should allow bridge to mint tokens", async function () {
      await token.connect(admin).grantBridgeRole(admin.address);

      await expect(
        token.connect(admin).bridgeMint(user1.address, ethers.parseEther("1000"), "ethereum", "0x123abc")
      )
        .to.emit(token, "BridgeMint")
        .withArgs(user1.address, ethers.parseEther("1000"), "ethereum", "0x123abc");
    });

    it("Should allow users to burn for bridge", async function () {
      await token.connect(platformWallet).mintForPurchase(user1.address, ethers.parseEther("1000"), "order_123");

      await expect(token.connect(user1).bridgeBurn(ethers.parseEther("500"), "polygon"))
        .to.emit(token, "BridgeBurn")
        .withArgs(user1.address, ethers.parseEther("500"), "polygon");

      expect(await token.balanceOf(user1.address)).to.equal(ethers.parseEther("500"));
    });
  });

  describe("Admin Functions", function () {
    it("Should allow admin to pause", async function () {
      await token.connect(admin).pause();
      expect(await token.paused()).to.be.true;
    });

    it("Should block transfers when paused", async function () {
      await token.connect(admin).enableTrading();
      await token.connect(platformWallet).mintForPurchase(user1.address, ethers.parseEther("1000"), "order_123");
      await token.connect(admin).pause();

      await expect(token.connect(user1).transfer(user2.address, ethers.parseEther("100"))).to.be.reverted;
    });

    it("Should allow admin to unpause", async function () {
      await token.connect(admin).pause();
      await token.connect(admin).unpause();
      expect(await token.paused()).to.be.false;
    });
  });
});
