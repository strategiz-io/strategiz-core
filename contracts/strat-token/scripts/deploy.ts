import { ethers, upgrades } from "hardhat";
import * as dotenv from "dotenv";

dotenv.config();

async function main() {
  const [deployer] = await ethers.getSigners();

  console.log("========================================");
  console.log("STRATEGIZ TOKEN (STRAT) DEPLOYMENT");
  console.log("========================================");
  console.log("Deployer:", deployer.address);
  console.log("Balance:", ethers.formatEther(await ethers.provider.getBalance(deployer.address)), "ETH");

  // For testnet, use deployer as all wallets (configure properly for mainnet)
  const admin = deployer.address;
  const platformWallet = process.env.PLATFORM_WALLET_ADDRESS || deployer.address;
  const liquidityWallet = process.env.LIQUIDITY_WALLET_ADDRESS || deployer.address;
  const developmentWallet = process.env.DEVELOPMENT_WALLET_ADDRESS || deployer.address;
  const marketingWallet = process.env.MARKETING_WALLET_ADDRESS || deployer.address;
  const teamWallet = process.env.TEAM_WALLET_ADDRESS || deployer.address;
  const treasuryWallet = process.env.TREASURY_WALLET_ADDRESS || deployer.address;

  console.log("\nWallet Configuration:");
  console.log("- Admin:", admin);
  console.log("- Platform (50%):", platformWallet);
  console.log("- Liquidity (20%):", liquidityWallet);
  console.log("- Development (15%):", developmentWallet);
  console.log("- Marketing (10%):", marketingWallet);
  console.log("- Team (5%):", teamWallet);
  console.log("- Treasury:", treasuryWallet);

  console.log("\nDeploying StrategizToken...");

  const StrategizToken = await ethers.getContractFactory("StrategizToken");

  const token = await upgrades.deployProxy(
    StrategizToken,
    [
      admin,
      platformWallet,
      liquidityWallet,
      developmentWallet,
      marketingWallet,
      teamWallet,
      treasuryWallet,
    ],
    {
      initializer: "initialize",
      kind: "uups",
    }
  );

  await token.waitForDeployment();

  const proxyAddress = await token.getAddress();
  const implementationAddress = await upgrades.erc1967.getImplementationAddress(proxyAddress);

  console.log("\n========================================");
  console.log("DEPLOYMENT SUCCESSFUL!");
  console.log("========================================");
  console.log("Proxy Address:", proxyAddress);
  console.log("Implementation:", implementationAddress);
  console.log("========================================");

  // Verify state
  const name = await token.name();
  const symbol = await token.symbol();
  const totalSupply = await token.totalSupply();
  const transfersRestricted = await token.transfersRestricted();

  console.log("\nToken Info:");
  console.log("- Name:", name);
  console.log("- Symbol:", symbol);
  console.log("- Total Supply:", ethers.formatEther(totalSupply), "STRAT");
  console.log("- Decimals: 18");
  console.log("- Phase:", transfersRestricted ? "UTILITY (transfers restricted)" : "TRADING");

  // Check allocations
  const platformBalance = await token.balanceOf(platformWallet);
  const liquidityBalance = await token.balanceOf(liquidityWallet);
  const devBalance = await token.balanceOf(developmentWallet);
  const marketingBalance = await token.balanceOf(marketingWallet);
  const teamBalance = await token.balanceOf(teamWallet);

  console.log("\nAllocations:");
  console.log("- Platform (50%):", ethers.formatEther(platformBalance), "STRAT");
  console.log("- Liquidity (20%):", ethers.formatEther(liquidityBalance), "STRAT");
  console.log("- Development (15%):", ethers.formatEther(devBalance), "STRAT");
  console.log("- Marketing (10%):", ethers.formatEther(marketingBalance), "STRAT");
  console.log("- Team (5%):", ethers.formatEther(teamBalance), "STRAT");

  console.log("\n========================================");
  console.log("NEXT STEPS:");
  console.log("========================================");
  console.log("1. Save proxy address to backend config");
  console.log("2. Verify on BaseScan:");
  console.log(`   npx hardhat verify --network baseSepolia ${proxyAddress}`);
  console.log("3. Configure backend to call mintForPurchase()");
  console.log("4. When ready for trading: call enableTrading()");
  console.log("");
  console.log("Verify on PolygonScan:");
  console.log(`   npx hardhat verify --network polygonAmoy ${proxyAddress}`);
  console.log("========================================");

  // Save deployment info
  const fs = await import("fs");
  const network = await ethers.provider.getNetwork();
  const deploymentInfo = {
    network: network.name,
    chainId: Number(network.chainId),
    proxyAddress,
    implementationAddress,
    admin,
    platformWallet,
    liquidityWallet,
    developmentWallet,
    marketingWallet,
    teamWallet,
    treasuryWallet,
    deployedAt: new Date().toISOString(),
    phase: "utility",
  };

  const filename = `deployment-${network.chainId}.json`;
  fs.writeFileSync(filename, JSON.stringify(deploymentInfo, null, 2));
  console.log(`\nDeployment info saved to ${filename}`);
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
