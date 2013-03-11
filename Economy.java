import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO show sign locations in offer list
// TODO allow item names OR id's
// TODO money in banks earn interest
// TODO add partial remove to offers
// TODO bank contents not working?
// TODO add user guide to README
// TODO change items names to items.txt
// TODO minimum area size
// TODO updatr support
// TODO consolidate item/money withdrawing and depositing
// TODO allow players to put areas up for offer
// TODO composite areas - see paper
// TODO fix colors when messages wrap

// TODO Companies to control paychecks

public class Economy extends Plugin implements Serializable {
	static final long serialVersionUID = 0L;
	static final Logger logger = Logger.getLogger("Minecraft");
	static final String PLUGIN_NAME = "The New Economy";
	static final String PLUGIN_VERSION = "1.7";

	/**
	 * Gets the running instance of the Economy plugin.
	 * 
	 * @return an instance of the plugin.
	 */
	public static final Economy getInstance() {
		return (Economy) etc.getLoader().getPlugin(Economy.PLUGIN_NAME);
	}

	/**
	 * The name of the properties file.
	 */
	private static final String PROPERTIES_NAME = "economy.properties";

	/**
	 * The File containing information about offers and balances.
	 */
	private static final File ECONOMY_FILE;

	/**
	 * The File containing information about trading areas and trading areas
	 * pending.
	 */
	private static final File LOCATIONS_FILE;

	/**
	 * The File containing information about items stored in banks.
	 */
	private static final File CHEST_FILE;

	/**
	 * A Map containing all the current commands for the mod.
	 */
	static final HashMap<String, String> COMMANDS = new HashMap<String, String>() {
		{
			this.put("/area",
					"- contains multiple commands for handling areas.");
			this.put("/buy", "- attempts to buy items up for offer.");
			this.put("/lottery", "- attempts to run a lottery.");
			this.put("/money",
					"- contains multiple commands for handling money.");
			this.put("/offer",
					"- contains multiple commands for handling offers.");
			this.put("/saveEconomy",
					"- saves the current state of the economy.");
			this.put("/auction",
					"- contains multiple commands for handling auctions.");
			this.put("/bank",
					"- contains multiple commands for handling bank accounts.");
		}
	};

	/**
	 * A map of all properties from economy.properties.
	 */
	public static Map<String, String> PROPERTIES;

	static {
		ECONOMY_FILE = new File("economy.dat");
		LOCATIONS_FILE = new File("locations.dat");
		CHEST_FILE = new File("chests.dat");
		try {
			if (ECONOMY_FILE.createNewFile()) {
				ObjectOutputStream outputStream = new ObjectOutputStream(
						new FileOutputStream(ECONOMY_FILE));
				// balances
				outputStream.writeObject(new HashMap<String, Double>());
				// paychecks
				outputStream.writeObject(new HashMap<String, Double>());
				// offers by name
				outputStream
						.writeObject(new HashMap<String, ArrayList<Offer>>());
				// all offers
				outputStream.writeObject(new HashSet<Offer>());
				// offer count
				outputStream.writeObject(new Integer(0));
				// public find
				outputStream.writeObject(new Double(0.0));
				// offer listeners
				outputStream
						.writeObject(new HashMap<Integer, HashSet<String>>());
				// last lottery
				outputStream.writeObject(new Long(System.currentTimeMillis()
						- (long) (60.0 * 60.0 * 1000.0)));
				// auction count
				outputStream.writeObject(new Integer(0));
				// auctions
				outputStream.writeObject(new TreeMap<String, Auction>());
				outputStream.flush();
				outputStream.close();
				logger.log(Level.INFO, "Economy file successfully created.");
			}
		} catch (IOException exception) {
			exception.printStackTrace();
			logger.log(Level.SEVERE, "Exception while creating economy file.");
		}
		try {
			if (LOCATIONS_FILE.createNewFile()) {
				ObjectOutputStream outputStream = new ObjectOutputStream(
						new FileOutputStream(LOCATIONS_FILE));
				// trading areas
				outputStream.writeObject(new HashSet<Area>());
				// personal areas
				outputStream.writeObject(new HashSet<Area>());
				// banking areas
				outputStream.writeObject(new HashSet<Area>());
				// pending trade areas
				outputStream
						.writeObject(new HashMap<String, ArrayList<Block>>());
				// pending personal areas
				outputStream
						.writeObject(new HashMap<String, ArrayList<Block>>());
				// pending bank areas
				outputStream
						.writeObject(new HashMap<String, ArrayList<Block>>());
				// area count
				outputStream.writeObject(new Integer(0));
				outputStream.flush();
				outputStream.close();
				logger.log(Level.INFO, "Locations file successfully created.");
			}
		} catch (IOException exception) {
			exception.printStackTrace();
			logger
					.log(Level.SEVERE,
							"Exception while creating locations file.");
		}
		try {
			if (CHEST_FILE.createNewFile()) {
				ObjectOutputStream outputStream = new ObjectOutputStream(
						new FileOutputStream(CHEST_FILE));
				// bank accounts
				outputStream
						.writeObject(new HashMap<String, TreeSet<BankSlot>>());
				outputStream.flush();
				outputStream.close();
			}
		} catch (IOException exception) {
			exception.printStackTrace();
			logger.log(Level.SEVERE, "Exception while creating chest file.");
		}
	}

	/**
	 * The maximum side length for a trading area.
	 */
	static int MAX_TRADE_AREA_LENGTH = 15;

	/**
	 * The maximum size for a trading area (in square blocks).
	 */
	static int MAX_TRADE_AREA_SIZE = 144;

	/**
	 * The maximum side length for a banking area.
	 */
	static int MAX_BANK_AREA_LENGTH = 25;

	/**
	 * The maximum size for a banking area (in square blocks).
	 */
	static int MAX_BANK_AREA_SIZE = 400;

	/**
	 * Cost per block for a trading area.
	 */
	static double TRADE_AREA_COST = 1.0;

	/**
	 * Cost per block for a personal area.
	 */
	static double PERSONAL_AREA_COST = 3.0;

	/**
	 * Cost per block for a banking area.
	 */
	static double BANK_AREA_COST = 5.0;

	/**
	 * Percent of area cost that must be paid for transfer.
	 */
	static double TRANSFER_FEE = 0.15;

	/**
	 * Percent of area cost that must be paid to rename an area (1st rename is
	 * free).
	 */
	static double RENAME_FEE = 0.05;

	/**
	 * The amount of money a player receives when they join the server.
	 */
	static double STARTING_MONEY = 10.0;

	/**
	 * The color for trade areas.
	 */
	static String TRADE_AREA = Colors.Yellow;

	/**
	 * The color for personal areas.
	 */
	static String PERSONAL_AREA_COLOR = Colors.LightBlue;

	/**
	 * The color for bank areas.
	 */
	static String BANK_AREA_COLOR = Colors.Green;

	/**
	 * The color for selling offers.
	 */
	static String OFFER = Colors.LightGray;

	/**
	 * The color for online players.
	 */
	static String ONLINE = Colors.White;

	/**
	 * The color for offline players.
	 */
	static String OFFLINE = Colors.Gray;

	/**
	 * The color for errors.
	 */
	static String ERROR = Colors.Red;

	/**
	 * The color for information.
	 */
	static String INFO = Colors.Blue;

	/**
	 * The color for command information.
	 */
	static String COMMAND = Colors.Rose;

	/**
	 * The color for money information.
	 */
	static String MONEY = Colors.Green;

	/**
	 * The maximum number of offers any player can have active at once.
	 */
	static int MAX_OFFERS = 10;

	/**
	 * The percentage of area cost that vertical space costs.
	 */
	static double VERTICAL_COST = 0.01;

	/**
	 * The time between lotteries in minutes.
	 */
	static double LOTTERY_INTERVAL = 60.0;

	/**
	 * The number of players online for the lottery winnings to be half of the
	 * public fund.
	 */
	static int PLAYERS_TO_HALF = 5;

	/**
	 * The maximum number of areas a player can own.
	 */
	static int MAX_AREAS = 20;

	/**
	 * The sales tax for buying items.
	 */
	static double SALES_TAX = 0.01;

	/**
	 * The minimum length for auctions in seconds.
	 */
	static double MIN_AUCTION_LENGTH = 15.0;

	/**
	 * The maximum length for auctions in seconds.
	 */
	static double MAX_AUCTION_LENGTH = 60.0;

	/**
	 * The number of results to display per page for listing commands.
	 */
	static int RESULTS_PER_PAGE = 6;

	/**
	 * A Map containing information about player balances.
	 */
	private HashMap<String, Double> balances;

	/**
	 * A Map containing information about player paychecks (unused).
	 */
	private HashMap<String, Double> paychecks;

	/**
	 * A Map containing information about current offers stored by player name.
	 */
	private HashMap<String, ArrayList<Offer>> sellingOffers;

	/**
	 * A Map containing information about trading areas that are still pending /
	 * being created.
	 */
	private HashMap<String, ArrayList<Block>> pendingTradeAreas;

	/**
	 * A Map containing information about banking areas that are still pending /
	 * being created.
	 */
	private HashMap<String, ArrayList<Block>> pendingBankAreas;

	/**
	 * A Set of all trading areas.
	 */
	private HashSet<Area> tradingAreas;

	/**
	 * A Map containing information about personal areas that are still pending
	 * / being created.
	 */
	private HashMap<String, ArrayList<Block>> pendingPersonalAreas;

	/**
	 * A Set of all personal areas.
	 */
	private HashSet<Area> personalAreas;

	/**
	 * A Set of all banking areas.
	 */
	private HashSet<Area> bankingAreas;

	/**
	 * A Set of all current offers.
	 */
	private HashSet<Offer> allOffers;

	/**
	 * The amount of money in the public fund.
	 */
	private double publicFund;

	/**
	 * The time of the last lottery.
	 */
	private long lastLottery;

	/**
	 * Maps item ID's to players listening for those to be put up for offer
	 */
	private HashMap<Integer, HashSet<String>> offerListeners;

	/**
	 * Maps player names to their current auction.
	 */
	private TreeMap<String, Auction> auctions;

	/**
	 * Maps player names to their bank accounts.
	 */
	private HashMap<String, TreeSet<BankSlot>> bankAccounts;

	/**
	 * The default values for items.
	 */
	static HashMap<Integer, Double> ITEM_VALUES = new HashMap<Integer, Double>() {
		{
			this.put(57, 900.0); // diamond block, not included in Item
			this.put(itemId("Diamond"), 100.0);
			this.put(itemId("GoldBlock"), 90.0);
			this.put(266, 10.0); // gold ingot, hard coded wrong
			this.put(itemId("IronBlock"), 9.0);
			this.put(itemId("IronIngot"), 1.0);
		}
	};

	@Override
	public void enable() {
		for (Map.Entry<String, String> currentCommand : Economy.COMMANDS
				.entrySet()) {
			etc.getInstance().addCommand(currentCommand.getKey(),
					currentCommand.getValue());
		}
		if (!this.isEnabled()) {
			this.toggleEnabled();
			logger.info(this + " enabled.");
		}
	}

	@Override
	public void disable() {
		for (Map.Entry<String, String> currentCommand : Economy.COMMANDS
				.entrySet()) {
			etc.getInstance().removeCommand(currentCommand.getKey());
		}
		this.save();
		if (this.isEnabled()) {
			this.toggleEnabled();
			logger.info(this + " disabled.");
		}
	}

	@Override
	public void initialize() {
		this.readEconomy();
		this.readLocations();
		this.readAccounts();
		try {
			this.readProperties();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		PluginListener listener = new EconomyListener(this);
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this,
				PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_RIGHTCLICKED,
				listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_PLACE, listener,
				this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_BROKEN, listener,
				this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.MOB_SPAWN, listener,
				this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this,
				PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.SIGN_CHANGE, listener,
				this, PluginListener.Priority.MEDIUM);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Economy.this.save();
			}
		});
		logger.info(this + " initialized.");
	}

	@Override
	public final String toString() {
		return PLUGIN_NAME + " version " + PLUGIN_VERSION;
	}

	/**
	 * Gets the amount of money in the publc fund.
	 * 
	 * @return the public fund.
	 */
	public final double getPublicFund() {
		return this.publicFund;
	}

	/**
	 * Determines if a player is online based on their name.
	 * 
	 * @param playerName
	 *            the player's name.
	 * @return true if they are online, false if they're not.
	 */
	private static final boolean online(String playerName) {
		return etc.getServer().getPlayer(playerName) != null;
	}

	/**
	 * Reads in the information stored in the economy file.
	 */
	@SuppressWarnings("unchecked")
	private final void readEconomy() {
		try {
			ObjectInputStream inputStream = new ObjectInputStream(
					new FileInputStream(Economy.ECONOMY_FILE));
			this.balances = (HashMap<String, Double>) inputStream.readObject();
			this.paychecks = (HashMap<String, Double>) inputStream.readObject();
			this.sellingOffers = (HashMap<String, ArrayList<Offer>>) inputStream
					.readObject();
			this.allOffers = (HashSet<Offer>) inputStream.readObject();
			Economy.offerCount = (Integer) inputStream.readObject();
			this.publicFund = (Double) inputStream.readObject();
			this.offerListeners = (HashMap<Integer, HashSet<String>>) inputStream
					.readObject();
			this.lastLottery = (Long) inputStream.readObject();
			Economy.auctionCount = (Integer) inputStream.readObject();
			this.auctions = (TreeMap<String, Auction>) inputStream.readObject();
			inputStream.close();
			logger.log(Level.INFO, "Economy file successfully read.");
		} catch (Exception ioe) {
			ioe.printStackTrace();
			logger.log(Level.SEVERE, "Error while reading economy file.");
		} finally {
			if (this.balances == null) {
				this.balances = new HashMap<String, Double>();
			}
			if (this.paychecks == null) {
				this.paychecks = new HashMap<String, Double>();
			}
			if (this.sellingOffers == null) {
				this.sellingOffers = new HashMap<String, ArrayList<Offer>>();
			}
			if (this.allOffers == null) {
				this.allOffers = new HashSet<Offer>();
			}
			if (this.offerListeners == null) {
				this.offerListeners = new HashMap<Integer, HashSet<String>>();
			}
			if (this.auctions == null) {
				this.auctions = new TreeMap<String, Auction>();
			}
		}
	}

	/**
	 * Reads in the information stored in the locations file.
	 */
	@SuppressWarnings("unchecked")
	private final void readLocations() {
		try {
			ObjectInputStream inputStream = new ObjectInputStream(
					new FileInputStream(Economy.LOCATIONS_FILE));
			this.tradingAreas = (HashSet<Area>) inputStream.readObject();
			this.personalAreas = (HashSet<Area>) inputStream.readObject();
			this.bankingAreas = (HashSet<Area>) inputStream.readObject();
			this.pendingTradeAreas = (HashMap<String, ArrayList<Block>>) inputStream
					.readObject();
			this.pendingPersonalAreas = (HashMap<String, ArrayList<Block>>) inputStream
					.readObject();
			this.pendingBankAreas = (HashMap<String, ArrayList<Block>>) inputStream
					.readObject();
			Economy.areaCount = (Integer) inputStream.readObject();
			inputStream.close();
			logger.log(Level.INFO, "Locations file successfully read.");
		} catch (Exception exception) {
			exception.printStackTrace();
			logger.log(Level.SEVERE, "Error while reading locations file.");
		} finally {
			if (this.tradingAreas == null) {
				this.tradingAreas = new HashSet<Area>();
			}
			if (this.personalAreas == null) {
				this.personalAreas = new HashSet<Area>();
			}
			if (this.bankingAreas == null) {
				this.bankingAreas = new HashSet<Area>();
			}
			if (this.pendingTradeAreas == null) {
				this.pendingTradeAreas = new HashMap<String, ArrayList<Block>>();
			}
			if (this.pendingPersonalAreas == null) {
				this.pendingPersonalAreas = new HashMap<String, ArrayList<Block>>();
			}
			if (this.pendingBankAreas == null) {
				this.pendingBankAreas = new HashMap<String, ArrayList<Block>>();
			}
		}
	}

	/**
	 * Reads in the information stored in the banking accounts file.
	 */
	@SuppressWarnings("unchecked")
	private final void readAccounts() {
		try {
			ObjectInputStream inputStream = new ObjectInputStream(
					new FileInputStream(CHEST_FILE));
			this.bankAccounts = (HashMap<String, TreeSet<BankSlot>>) inputStream
					.readObject();
			logger.log(Level.INFO, "Bank accounts file successfully read.");
		} catch (Exception exception) {
			exception.printStackTrace();
			logger.log(Level.SEVERE, "Error while reading bank accounts file.");
		} finally {
			if (this.bankAccounts == null) {
				this.bankAccounts = new HashMap<String, TreeSet<BankSlot>>();
			}
		}
	}

	/**
	 * Loads all properties from the economy.properties file.
	 * 
	 * @throws Exception
	 */
	private final void readProperties() throws Exception {
		if (PROPERTIES != null) {
			return;
		}
		new File(PROPERTIES_NAME).createNewFile();
		PROPERTIES = new PropertiesFile(PROPERTIES_NAME).returnMap();
		boolean error = false;
		PrintWriter output = new PrintWriter(new FileWriter(new File(
				PROPERTIES_NAME), true));
		if (PROPERTIES.containsKey("maxTradeAreaLength")) {
			try {
				MAX_TRADE_AREA_LENGTH = Integer.parseInt(PROPERTIES
						.get("maxTradeAreaLength"));
				if (MAX_TRADE_AREA_LENGTH < 1) {
					MAX_TRADE_AREA_LENGTH = 1;
				}

			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output.println("# The maximum side length for a trade area.");
			output.println("maxTradeAreaLength=15");
		}
		if (PROPERTIES.containsKey("maxTradeAreaSize")) {
			try {
				MAX_TRADE_AREA_SIZE = Integer.parseInt(PROPERTIES
						.get("maxTradeAreaSize"));
				if (MAX_TRADE_AREA_SIZE < 1) {
					MAX_TRADE_AREA_SIZE = 1;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The maximum size of a trade area in square blocks.");
			output.println("maxTradeAreaSize=144");
		}
		if (PROPERTIES.containsKey("maxBankAreaLength")) {
			try {
				MAX_BANK_AREA_LENGTH = Integer.parseInt(PROPERTIES
						.get("maxBankAreaLength"));
				if (MAX_BANK_AREA_LENGTH < 1) {
					MAX_BANK_AREA_LENGTH = 1;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output.println("\n# The maximum side length for a bank area.");
			output.println("maxBankAreaLength=25");
		}
		if (PROPERTIES.containsKey("maxBankAreaSize")) {
			try {
				MAX_BANK_AREA_SIZE = Integer.parseInt(PROPERTIES
						.get("maxBankAreaSize"));
				if (MAX_BANK_AREA_SIZE < 1) {
					MAX_BANK_AREA_SIZE = 1;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The maximum size of a bank area in square blocks.");
			output.println("maxBankAreaSize=400");
		}
		if (PROPERTIES.containsKey("tradeAreaCost")) {
			try {
				TRADE_AREA_COST = Double.parseDouble(PROPERTIES
						.get("tradeAreaCost"));
				if (TRADE_AREA_COST < 0.0) {
					TRADE_AREA_COST = 0.0;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output.println("\n# The cost per square block for a trade area.");
			output.println("tradeAreaCost=1.0");
		}
		if (PROPERTIES.containsKey("personalAreaCost")) {
			try {
				PERSONAL_AREA_COST = Double.parseDouble(PROPERTIES
						.get("personalAreaCost"));
				if (PERSONAL_AREA_COST < 0.0) {
					PERSONAL_AREA_COST = 0.0;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The cost per square block for a personal area.");
			output.println("personalAreaCost=3.0");
		}
		if (PROPERTIES.containsKey("bankAreaCost")) {
			try {
				BANK_AREA_COST = Double.parseDouble(PROPERTIES
						.get("bankAreaCost"));
				if (BANK_AREA_COST < 0.0) {
					BANK_AREA_COST = 0.0;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output.println("\n# The cost per square block for a bank area.");
			output.println("bankAreaCost=5.0");
		}
		if (PROPERTIES.containsKey("transferFee")) {
			try {
				TRANSFER_FEE = Double
						.parseDouble(PROPERTIES.get("transferFee"));
				if (TRANSFER_FEE > 1.0) {
					TRANSFER_FEE = 1.0;
				} else if (TRANSFER_FEE < 0.0) {
					TRANSFER_FEE = 0.0;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The percent of an area's cost that is paid for transfers.");
			output.println("transferFee=0.15");
		}
		if (PROPERTIES.containsKey("renameFee")) {
			try {
				RENAME_FEE = Double.parseDouble(PROPERTIES.get("renameFee"));
				if (RENAME_FEE > 1.0) {
					RENAME_FEE = 1.0;
				} else if (RENAME_FEE < 0.0) {
					RENAME_FEE = 0.0;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The percent of an area's cost that is paid for renaming.");
			output.println("renameFee=0.05");
		}
		if (PROPERTIES.containsKey("salesTax")) {
			try {
				SALES_TAX = Double.parseDouble(PROPERTIES.get("salesTax"));
				if (SALES_TAX > 1.0) {
					SALES_TAX = 1.0;
				} else if (SALES_TAX < 0.0) {
					SALES_TAX = 0.0;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The percent of a transaction charge that goes to the public fund.");
			output.println("salesTax=0.01");
		}
		if (PROPERTIES.containsKey("startingMoney")) {
			try {
				STARTING_MONEY = Double.parseDouble(PROPERTIES
						.get("startingMoney"));
				if (STARTING_MONEY < 0) {
					STARTING_MONEY = 0;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The amount of money given to new players from the public fund.");
			output.println("startingMoney=10.0");
		}
		if (PROPERTIES.containsKey("tradeAreaColor")) {
			try {
				TRADE_AREA = (String) Colors.class.getField(
						PROPERTIES.get("tradeAreaColor")).get(null);
			} catch (Exception exception) {
				error = true;
				exception.printStackTrace();
			}
		} else {
			output.println("\n# The color for displaying trade areas.");
			output.println("tradeAreaColor=Yellow");
		}
		if (PROPERTIES.containsKey("personalAreaColor")) {
			try {
				PERSONAL_AREA_COLOR = (String) Colors.class.getField(
						PROPERTIES.get("personalAreaColor")).get(null);
			} catch (Exception exception) {
				error = true;
				exception.printStackTrace();
			}
		} else {
			output.println("\n# The color for displaying personal areas.");
			output.println("personalAreaColor=LightBlue");
		}
		if (PROPERTIES.containsKey("bankAreaColor")) {
			try {
				BANK_AREA_COLOR = (String) Colors.class.getField(
						PROPERTIES.get("bankAreaColor")).get(null);
			} catch (Exception exception) {
				error = true;
				exception.printStackTrace();
			}
		} else {
			output.println("\n# The color for displaying bank areas.");
			output.println("bankAreaColor=Green");
		}
		if (PROPERTIES.containsKey("offerColor")) {
			try {
				OFFER = (String) Colors.class.getField(
						PROPERTIES.get("offerColor")).get(null);
			} catch (Exception exception) {
				error = true;
				exception.printStackTrace();
			}
		} else {
			output.println("\n# The color for displaying offers.");
			output.println("offerColor=LightGray");
		}
		if (PROPERTIES.containsKey("errorColor")) {
			try {
				ERROR = (String) Colors.class.getField(
						PROPERTIES.get("errorColor")).get(null);
			} catch (Exception exception) {
				error = true;
				exception.printStackTrace();
			}
		} else {
			output.println("\n# The color for displaying errors.");
			output.println("errorColor=Red");
		}
		if (PROPERTIES.containsKey("commandColor")) {
			try {
				COMMAND = (String) Colors.class.getField(
						PROPERTIES.get("commandColor")).get(null);
			} catch (Exception exception) {
				error = true;
				exception.printStackTrace();
			}
		} else {
			output.println("\n# The color for displaying command usages.");
			output.println("commandColor=Rose");
		}
		if (PROPERTIES.containsKey("infoColor")) {
			try {
				INFO = (String) Colors.class.getField(
						PROPERTIES.get("infoColor")).get(null);
			} catch (Exception exception) {
				error = true;
				exception.printStackTrace();
			}
		} else {
			output.println("\n# The color for displaying information.");
			output.println("infoColor=Blue");
		}
		if (PROPERTIES.containsKey("moneyColor")) {
			try {
				MONEY = (String) Colors.class.getField(
						PROPERTIES.get("moneyColor")).get(null);
			} catch (Exception exception) {
				error = true;
				exception.printStackTrace();
			}
		} else {
			output.println("\n# The color for displaying money.");
			output.println("moneyColor=Green");
		}
		if (PROPERTIES.containsKey("onlineColor")) {
			try {
				ONLINE = (String) Colors.class.getField(
						PROPERTIES.get("onlineColor")).get(null);
			} catch (Exception exception) {
				error = true;
				exception.printStackTrace();
			}
		} else {
			output.println("\n# The color for player names who are online.");
			output.println("onlineColor=White");
		}
		if (PROPERTIES.containsKey("offlineColor")) {
			try {
				OFFLINE = (String) Colors.class.getField(
						PROPERTIES.get("offlineColor")).get(null);
			} catch (Exception exception) {
				error = true;
				exception.printStackTrace();
			}
		} else {
			output.println("\n# The color for player names who are offline.");
			output.println("offlineColor=Gray");
		}
		if (PROPERTIES.containsKey("maxOffers")) {
			try {
				MAX_OFFERS = Integer.parseInt(PROPERTIES.get("maxOffers"));
				if (MAX_OFFERS < 1) {
					MAX_OFFERS = 1;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The maximum number of offers a player can have.");
			output.println("maxOffers=10");
		}
		if (PROPERTIES.containsKey("verticalCost")) {
			try {
				VERTICAL_COST = Double.parseDouble(PROPERTIES
						.get("verticalCost"));
				if (VERTICAL_COST > 1.0) {
					VERTICAL_COST = 1.0;
				} else if (VERTICAL_COST < 0.0) {
					VERTICAL_COST = 0.0;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The percentage of floor space cost for each block of vertical space.");
			output.println("verticalCost=0.02");
		}
		if (PROPERTIES.containsKey("lotteryInterval")) {
			try {
				LOTTERY_INTERVAL = Double.parseDouble(PROPERTIES
						.get("lotteryInterval"));
				if (LOTTERY_INTERVAL < 0.0) {
					LOTTERY_INTERVAL = 0.0;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The minimum time between lotteries in minutes.");
			output.println("lotteryInterval=60");
		}
		if (PROPERTIES.containsKey("playersToHalf")) {
			try {
				PLAYERS_TO_HALF = Integer.parseInt(PROPERTIES
						.get("playersToHalf"));
				if (PLAYERS_TO_HALF < 0) {
					PLAYERS_TO_HALF = 0;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The number of players online for the lottery to be half of the public fund.");
			output.println("playersToHalf=5");
		}
		if (PROPERTIES.containsKey("maxAreas")) {
			try {
				MAX_AREAS = Integer.parseInt(PROPERTIES.get("maxAreas"));
				if (MAX_AREAS < 1) {
					MAX_AREAS = 1;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output.println("\n# The maximum number of areas a player can own.");
			output.println("maxAreas=20");
		}
		if (PROPERTIES.containsKey("exchange")) {
			ITEM_VALUES = new HashMap<Integer, Double>();
			for (String kv : PROPERTIES.get("exchange").split(",")) {
				int colon = kv.indexOf(':');
				if (colon == -1) {
					continue;
				}
				try {
					int id = Integer.parseInt(kv.substring(0, colon));
					double value = Double.parseDouble(kv.substring(colon + 1));
					value = Math.round(value * 100.0) / 100.0;
					if (value > 0) {
						ITEM_VALUES.put(id, value);
					}
				} catch (Exception e) {
					error = true;
					e.printStackTrace();
				}
			}
		} else {
			output.println("\n# The currency in the form itemId:value.");
			output.println("exchange=265:1,42:9,266:10,41:90,264:100,57:900");
		}
		if (PROPERTIES.containsKey("minAuctionLength")) {
			try {
				MIN_AUCTION_LENGTH = Double.parseDouble(PROPERTIES
						.get("minAuctionLength"));
				if (MIN_AUCTION_LENGTH < 0.0) {
					MIN_AUCTION_LENGTH = 0.0;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output.println("\n# The minimum length of an auction in seconds.");
			output.println("minAuctionLength=15.0");
		}
		if (PROPERTIES.containsKey("maxAuctionLength")) {
			try {
				MAX_AUCTION_LENGTH = Double.parseDouble(PROPERTIES
						.get("maxAuctionLength"));
				if (MAX_AUCTION_LENGTH < MIN_AUCTION_LENGTH) {
					MAX_AUCTION_LENGTH = MIN_AUCTION_LENGTH;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output.println("\n# The maximum length of an auction in seconds.");
			output.println("maxAuctionLength=60.0");
		}
		if (PROPERTIES.containsKey("resultsPerPage")) {
			try {
				RESULTS_PER_PAGE = Integer.parseInt(PROPERTIES
						.get("resultsPerPage"));
				if (RESULTS_PER_PAGE < 1) {
					RESULTS_PER_PAGE = 1;
				}
			} catch (Exception e) {
				error = true;
				e.printStackTrace();
			}
		} else {
			output
					.println("\n# The number of results to display per page for listing commands.");
			output.println("resultsPerPage=6");
		}
		if (error) {
			logger.log(Level.SEVERE, "Error while reading properties file.");
		} else {
			logger.log(Level.INFO, "Properties file successfully read.");
		}
		output.flush();
		output.close();
	}

	/**
	 * Writes the state of all relevant variables to the economy file.
	 */
	private final void writeEconomy() {
		ObjectOutputStream outputStream;
		try {
			outputStream = new ObjectOutputStream(new FileOutputStream(
					Economy.ECONOMY_FILE));
			outputStream.writeObject(this.balances);
			outputStream.writeObject(this.paychecks);
			outputStream.writeObject(this.sellingOffers);
			outputStream.writeObject(this.allOffers);
			outputStream.writeObject(new Integer(Economy.offerCount));
			outputStream.writeObject(new Double(this.publicFund));
			outputStream.writeObject(this.offerListeners);
			outputStream.writeObject(this.lastLottery);
			outputStream.writeObject(new Integer(Economy.auctionCount));
			outputStream.writeObject(this.auctions);
			outputStream.flush();
			outputStream.close();
			logger.log(Level.INFO, "Economy file successfully written.");
		} catch (Exception exception) {
			exception.printStackTrace();
			logger.log(Level.SEVERE, "Error while writing economy file.");
		}
	}

	/**
	 * Writes the state of all relevant variables to the locations file.
	 */
	private final void writeLocations() {
		ObjectOutputStream outputStream;
		try {
			outputStream = new ObjectOutputStream(new FileOutputStream(
					Economy.LOCATIONS_FILE));
			outputStream.writeObject(this.tradingAreas);
			outputStream.writeObject(this.personalAreas);
			outputStream.writeObject(this.bankingAreas);
			outputStream.writeObject(this.pendingTradeAreas);
			outputStream.writeObject(this.pendingPersonalAreas);
			outputStream.writeObject(this.pendingBankAreas);
			outputStream.writeObject(new Integer(Economy.areaCount));
			outputStream.flush();
			outputStream.close();
			logger.log(Level.INFO, "Locations file successfully written.");
		} catch (Exception exception) {
			exception.printStackTrace();
			logger.log(Level.SEVERE, "Error while writing locations file.");
		}
	}

	/**
	 * Writes the state of all bank accounts to the bank accounts file.
	 */
	private final void writeAccounts() {
		try {
			ObjectOutputStream outputStream = new ObjectOutputStream(
					new FileOutputStream(Economy.CHEST_FILE));
			outputStream.writeObject(this.bankAccounts);
			outputStream.flush();
			outputStream.close();
			logger.log(Level.INFO, "Bank accounts file successfully written.");
		} catch (Exception exception) {
			exception.printStackTrace();
			logger.log(Level.SEVERE, "Error while writing bank accounts file.");
		}
	}

	/**
	 * Gets the iterator for all current offers.
	 * 
	 * @return an iterator over all offers.
	 */
	public final Iterator<Offer> offerIterator() {
		return this.allOffers.iterator();
	}

	/**
	 * Returns the balance for the given player.
	 * 
	 * @param player
	 *            the player.
	 * @return the player's balance, or 0 if they didn't have one.
	 */
	public final double getBalance(Player player) {
		return this.getBalance(player.getName());
	}

	/**
	 * Returns the balance for the given player.
	 * 
	 * @param player
	 *            the player.
	 * @return the player's balance, or 0 if they didn't have one.
	 */
	public final double getBalance(String player) {
		Double balance = this.balances.get(player.toLowerCase());
		if (balance == null) {
			this.balances.put(player.toLowerCase(), 0.0);
			balance = 0.0;
		}
		return balance;
	}

	/**
	 * Sets the balance for the given player.
	 * 
	 * @param player
	 *            the player.
	 * @param balance
	 *            the balance to set to.
	 */
	public final void setBalance(Player player, double balance) {
		this.setBalance(player.getName(), balance);
	}

	/**
	 * Sets the balance for the given player.
	 * 
	 * @param player
	 *            the player.
	 * @param balance
	 *            the balance to set to.
	 */
	public final void setBalance(String player, double balance) {
		assert balance >= 0
				&& (this.balances.containsKey(player) || etc.getServer()
						.getPlayer(player) != null);
		this.balances.put(player.toLowerCase(), balance);
	}

	/**
	 * Adds the specified amount to the given player's balance.
	 * 
	 * @param player
	 *            the player.
	 * @param amount
	 *            the amount to add (or remove if negative).
	 */
	public final void addToBalance(Player player, double amount) {
		this.addToBalance(player.getName(), amount);
	}

	/**
	 * Adds the specified amount to the given player's balance.
	 * 
	 * @param player
	 *            the player.
	 * @param amount
	 *            the amount to add (or remove if negative).
	 */
	public final void addToBalance(String player, double amount) {
		if (this.getBalance(player) > Double.MAX_VALUE - amount) {
			// could potentially overflow, limit results
			if (amount < 0) {
				this.setBalance(player, Math.max(0, this.getBalance(player)
						+ amount));
			} else {
				this.setBalance(player, Double.MAX_VALUE);
			}
		} else {
			// guarenteed to not cause overflow
			this.setBalance(player, Math.max(0, this.getBalance(player)
					+ amount));
		}
	}

	/*
	 * 
	 * @SuppressWarnings("unused") final void setPaycheck(String player, int
	 * paycheck) { assert paycheck >= 0 && (etc.getServer().getPlayer(player) !=
	 * null || this.balances .containsKey(player));
	 * this.paychecks.put(player.toLowerCase(), paycheck); }
	 * 
	 * final int getPaycheck(String player) { Integer paycheck =
	 * this.paychecks.get(player.toLowerCase()); if (paycheck == null) {
	 * this.paychecks.put(player.toLowerCase(), 0); paycheck = 0; } return
	 * paycheck; }
	 * 
	 * @SuppressWarnings("unused") final void pay(String player) { assert
	 * this.paychecks.containsKey(player.toLowerCase()) ||
	 * this.balances.containsKey(player.toLowerCase()) ||
	 * etc.getServer().getPlayer(player.toLowerCase()) != null;
	 * this.addToBalance(player, this.getPaycheck(player)); }
	 */

	/**
	 * Adds an offer for the given player.
	 * 
	 * @param player
	 *            the player.
	 * @param offer
	 *            the offer to add.
	 */
	public final void addOffer(Player player, Offer offer) {
		this.addOffer(player.getName(), offer);
	}

	/**
	 * Adds an offer for the given player.
	 * 
	 * @param player
	 *            the player.
	 * @param offer
	 *            the offer to add.
	 */
	public final void addOffer(String player, Offer offer) {
		ArrayList<Offer> playerOffers = this.sellingOffers.get(player
				.toLowerCase());
		if (playerOffers == null) {
			playerOffers = new ArrayList<Offer>();
			this.sellingOffers.put(player.toLowerCase(), playerOffers);
		}
		playerOffers.add(offer);
		this.allOffers.add(offer);
	}

	/**
	 * Removes the given offer.
	 * 
	 * @param offer
	 *            the offer to remove.
	 */
	public final void removeOffer(Offer offer) {
		Economy.this.sellingOffers.get(offer.owner).remove(offer);
		Economy.this.allOffers.remove(offer);
	}

	/**
	 * Determines whether there are currently offers available.
	 * 
	 * @return true if there are offers, false otherwise.
	 */
	public final boolean hasOffers() {
		return !this.allOffers.isEmpty();
	}

	/**
	 * Gets the name of the item with the given id.
	 * 
	 * @param id
	 *            the item id.
	 * @return the name of the item with the id.
	 */
	private static final String itemName(int id) {
		return Item.Type.fromId(id).name().toLowerCase();
	}

	/**
	 * Gets the id of the item with the given name.
	 * 
	 * @param name
	 *            the item name.
	 * @return the id of the item with the name, or -1 if not found.
	 */
	private static final int itemId(String name) {
		for (Item.Type type : Item.Type.values()) {
			if (type.name().equalsIgnoreCase(name)) {
				return type.getId();
			}
		}
		return -1;
	}

	/**
	 * Gets the Area the specified player is in.
	 * 
	 * @param player
	 *            the player.
	 * @return the Area the player is in, or null if they aren't currenly in an
	 *         Area.
	 */
	public final Area getArea(String player) {
		Player p = etc.getServer().getPlayer(player);
		if (p == null) {
			return null;
		} else {
			return this.getArea(p);
		}
	}

	/**
	 * Gets the Area the specified player is in.
	 * 
	 * @param player
	 *            the player.
	 * @return the Area the player is in, or null if they aren't currenly in an
	 *         Area.
	 */
	public final Area getArea(Player player) {
		return this.getArea(player.getX(), player.getY(), player.getZ());
	}

	/**
	 * Gets the Area for the specified location.
	 * 
	 * @param l
	 *            the location.
	 * @return the Area for the location, or null if there are no areas
	 *         containing that location.
	 */
	public final Area getArea(Location l) {
		return this.getArea(l.x, l.y, l.z);
	}

	/**
	 * Gets the Area for the specified point.
	 * 
	 * @param x
	 *            the x-coordinate.
	 * @param z
	 *            the z-coordinate.
	 * @return the Area for the point, or null if there are no areas containing
	 *         that point.
	 */
	public final Area getArea(double x, double y, double z) {
		for (Area area : this.tradingAreas) {
			if (area.withinArea(x, y, z)) {
				return area;
			}
		}
		for (Area area : this.personalAreas) {
			if (area.withinArea(x, y, z)) {
				return area;
			}
		}
		for (Area area : this.bankingAreas) {
			if (area.withinArea(x, y, z)) {
				return area;
			}
		}
		return null;
	}

	/**
	 * Gets a list of all trading areas, personal areas, and banking areas.
	 * 
	 * @return a list of all areas.
	 */
	public final ArrayList<Area> allAreas() {
		return new ArrayList<Area>(this.tradingAreas.size()
				+ this.personalAreas.size() + this.bankingAreas.size()) {
			{
				this.addAll(Economy.this.tradingAreas);
				this.addAll(Economy.this.personalAreas);
				this.addAll(Economy.this.bankingAreas);
			}
		};
	}

	/**
	 * Determines if the given player can trade. A player can trade only if
	 * they're inside a trading area.
	 * 
	 * @param player
	 *            the player.
	 * @return true if the player can trade, false otherwise.
	 */
	public final boolean canTrade(Player player) {
		Area area = this.getArea(player);
		if (area == null) {
			return false;
		}
		return area instanceof TradeArea;
	}

	/**
	 * Formats a double for displaying currency.
	 * 
	 * @param amount
	 *            the amount to format.
	 * @return a formatted String.
	 */
	private static final String format(double amount) {
		return String.format("%.2f", amount);
	}

	/**
	 * Formats a parameter in hh:mm:ss
	 * 
	 * @param time
	 *            the time to format.
	 * @return a formatted time.
	 */
	private static final String formatTime(double time) {
		int hours = (int) (time / 60.0) % 24;
		int minutes = ((int) time) % 60;
		int seconds = (int) (60.0 * (time - (int) time) + 0.5);
		if (hours != 0) {
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		} else {
			return String.format("%02d:%02d", minutes, seconds);
		}
	}

	/**
	 * Gets the color for the specified area. By default, trade areas are
	 * yellow, personal areas are light blue, and bank areas are green.
	 * 
	 * @param area
	 *            an Area.
	 * @return the color for that area.
	 */
	public static final String areaColor(Area area) {
		if (area instanceof TradeArea) {
			return TRADE_AREA;
		} else if (area instanceof PersonalArea) {
			return PERSONAL_AREA_COLOR;
		} else if (area instanceof BankArea) {
			return BANK_AREA_COLOR;
		} else {
			return Colors.Black;
		}
	}

	/**
	 * Sends a message to a player which preserves colors on line breaks.
	 * 
	 * @param player
	 *            the player to send the message to.
	 * @param message
	 *            the message to send.
	 */
	private static final void sendMessage(Player player, String message) {
		/*
		 * StringBuilder sendingMessage = new StringBuilder(); String
		 * previousColor = Colors.White; char[] chars = message.toCharArray();
		 * for (int index = 0; index < message.length(); ++index) { if
		 * (chars[index] == '§') { previousColor = "§" + chars[index + 1]; } //
		 * TODO figure out color wrap. sendingMessage.append(chars[index]); }
		 * player.sendMessage(sendingMessage.toString());
		 */
		player.sendMessage(message);
	}

	/**
	 * Sends a message to everyone on the server.
	 * 
	 * @param message
	 *            the message to send.
	 */
	private static final void sendAll(String message) {
		for (Player p : etc.getServer().getPlayerList()) {
			sendMessage(p, message);
		}
	}

	/**
	 * Converts an array of parameters into key:value pairs.
	 * 
	 * @param split
	 *            parameters.
	 * @return key:value pairs for parameters.
	 */
	private static final HashMap<String, String> toKV(String[] split) {
		final HashMap<String, String> result = new HashMap<String, String>();
		for (String current : split) {
			int colon = current.indexOf(':');
			if (colon == -1) {
				continue;
			}
			String key = current.substring(0, colon).toLowerCase();
			String value = current.substring(colon + 1).toLowerCase();
			result.put(key, value);
		}
		return result;
	}

	/**
	 * Counts the number of the specified item the player is holding.
	 * 
	 * @param player
	 *            the player.
	 * @param itemId
	 *            the item id for the item being checked.
	 * @return the number of the specified item in in the player's inventory.
	 */
	public static final int countItem(Player player, int itemId) {
		int totalAmount = 0;
		Inventory inventory = player.getInventory();
		Item[] inventoryArray = inventory.getContents();

		for (int index = 0; index < inventoryArray.length; ++index) {
			if (inventoryArray[index] == null) {
				continue;
			}
			if (inventoryArray[index].getItemId() == itemId) {
				totalAmount += inventoryArray[index].getAmount();
			}
		}
		return totalAmount;
	}

	/**
	 * Gets the number of areas the specified player owns.
	 * 
	 * @param player
	 *            the player.
	 * @return the number of areas that player owns.
	 */
	public final int countAreas(String player) {
		Player p = etc.getServer().getPlayer(player);
		if (p == null) {
			return 0;
		} else {
			return this.countAreas(p);
		}
	}

	/**
	 * Gets the number of areas the specified player owns.
	 * 
	 * @param player
	 *            the player.
	 * @return the number of areas that player owns.
	 */
	public final int countAreas(Player player) {
		int result = 0;
		for (Area area : this.allAreas()) {
			if (player.getName().equalsIgnoreCase(area.owner)) {
				++result;
			}
		}
		return result;
	}

	/**
	 * Gets the player's bank account.
	 * 
	 * @param player
	 *            the player.
	 * @return the player's bank account.
	 */
	public final TreeSet<BankSlot> getAccount(String player) {
		Player p = etc.getServer().getPlayer(player);
		if (p == null) {
			return new TreeSet<BankSlot>();
		} else {
			return this.getAccount(p);
		}
	}

	/**
	 * Gets the player's bank account.
	 * 
	 * @param player
	 *            the player.
	 * @return the player's bank account.
	 */
	public final TreeSet<BankSlot> getAccount(Player player) {
		TreeSet<BankSlot> result = this.bankAccounts.get(player.getName()
				.toLowerCase());
		if (result == null) {
			result = new TreeSet<BankSlot>();
			this.bankAccounts.put(player.getName().toLowerCase(), result);
		}
		return result;
	}

	/**
	 * Removes a player's auction.
	 * 
	 * @param player
	 *            the player's name.
	 */
	public final void removeAuction(String player) {
		this.auctions.remove(player.toLowerCase());
	}

	/**
	 * Removes a player's auction.
	 * 
	 * @param player
	 *            the player.
	 */
	public final void removeAuction(Player player) {
		this.removeAuction(player.getName());
	}

	/**
	 * The global offer counter.
	 */
	private static int offerCount = 0;

	/**
	 * 
	 * The offer class represents a selling offer for a certain amount of an
	 * item.
	 * 
	 * @author lackeybp. Created Dec 22, 2010.
	 */
	public class Offer implements Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * The owner of this offer.
		 */
		public final String owner;

		/**
		 * The item id for this offer.
		 */
		public final int itemId;

		/**
		 * The price per unit for this offer.
		 */
		public final double unitPrice;

		/**
		 * The amount of items this offer is for.
		 */
		private int amount;

		/**
		 * The unique offer id for this offer.
		 */
		public final int offerId;

		/**
		 * The coordinates of this offer's selling sign if any.
		 */
		private int signX, signY, signZ;

		/**
		 * True if this offer has a sign associated with it.
		 */
		private boolean hasSign;

		public final Sign sellingSign() {
			if (this.hasSign) {
				return (Sign) etc.getServer().getComplexBlock(this.signX,
						this.signY, this.signZ);
			} else {
				return null;
			}
		}

		public final void setSign(Sign sign) {
			this.signX = sign.getX();
			this.signY = sign.getY();
			this.signZ = sign.getZ();
			this.hasSign = true;
		}

		/**
		 * Creates an offer with the specified parameters.
		 * 
		 * @param player
		 *            the owner of this offer.
		 * @param id
		 *            the item id for this offer.
		 * @param amount
		 *            the amount this offer is for.
		 * @param unitPrice
		 *            the price per unit for this offer.
		 */
		public Offer(String player, int id, int amount, double unitPrice) {
			this.owner = player;
			this.itemId = id;
			this.amount = amount;
			this.unitPrice = unitPrice;
			this.offerId = Economy.offerCount++;
			if (Economy.offerCount < 0) {
				Economy.logger.log(Level.WARNING,
						"Offer id count has overflowed.");
			}
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Offer)) {
				return false;
			}
			Offer otherOffer = (Offer) other;
			return this.offerId == otherOffer.offerId;
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append("(");
			result.append(this.amount);
			result.append(Economy.itemName(this.itemId));
			result.append(",");
			result.append(Economy.MONEY);
			result.append(Economy.format(this.unitPrice));
			result.append(Economy.OFFER);
			result.append(")");
			return result.toString();
		}

		/**
		 * Gets the amount of items in this offer.
		 * 
		 * @return the amount.
		 */
		public final int getAmount() {
			return this.amount;
		}
	}

	private static int auctionCount;

	public class Auction implements Serializable {
		private static final long serialVersionUID = 10L;
		private final Economy economy;
		public final String owner;
		public final int itemId, amount;
		public final int auctionId;

		private double currentBid;
		private String highestBidder;

		private boolean running;
		private boolean finished;
		private long startTime;

		double auctionLength = Economy.MAX_AUCTION_LENGTH;

		public Auction(Economy economy, String owner, int itemId, int amount,
				double startingBid) {
			this.economy = economy;
			this.owner = owner;
			this.itemId = itemId;
			this.amount = amount;
			this.currentBid = startingBid;
			this.auctionId = Economy.auctionCount++;
		}

		/**
		 * Returns true if this auction is running (active).
		 * 
		 * @return if this auction is running.
		 */
		public final boolean isRunning() {
			return this.running;
		}

		/**
		 * Returns true if this auction has finished (inactive).
		 * 
		 * @return if this auction has finished.
		 */
		public final boolean isFinished() {
			return this.finished;
		}

		/**
		 * Gets the current highest bid.
		 * 
		 * @return the highest bid.
		 */
		public final double currentBid() {
			return this.currentBid;
		}

		/**
		 * Gets the current highest bidder.
		 * 
		 * @return the highest bidder.
		 */
		public final String highestBidder() {
			return this.highestBidder;
		}

		/**
		 * Starts this auction.
		 */
		public void start() {
			this.running = true;
			this.finished = false;
			this.startTime = System.currentTimeMillis();
			Economy.sendAll(Economy.ONLINE + this.owner + Economy.INFO
					+ " has started an auction for " + Colors.White
					+ this.amount + " " + Economy.itemName(this.itemId)
					+ Economy.INFO + "! The bidding will start at "
					+ Economy.MONEY + Economy.format(this.currentBid)
					+ Economy.INFO + ". There is " + Colors.White
					+ Economy.formatTime(this.auctionLength / 60.0)
					+ Economy.INFO + " remaining. [" + Colors.White
					+ this.auctionId + Economy.INFO + "]");
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread
								.sleep((long) (Auction.this.auctionLength * 1000.0));
						if (Auction.this.finished) {
							return;
						}
						Auction.this.running = false;
						Auction.this.finished = true;
						Player owner = etc.getServer().getPlayer(
								Auction.this.owner);
						if (Auction.this.highestBidder == null) {
							Economy
									.sendAll(Economy.INFO
											+ "Nobody has bid on "
											+ (Economy
													.online(Auction.this.owner) ? Economy.ONLINE
													: Economy.OFFLINE)
											+ Auction.this.owner
											+ "\'s"
											+ Economy.INFO
											+ " auction for "
											+ Colors.White
											+ Auction.this.amount
											+ " "
											+ Economy
													.itemName(Auction.this.itemId)
											+ Economy.INFO + ".");
							if (owner != null) {
								owner.giveItem(Auction.this.itemId,
										Auction.this.amount);
								Auction.this.economy
										.removeAuction(Auction.this.owner);
							}
							return;
						} else if (!Economy.online(Auction.this.highestBidder)) {
							Economy
									.sendMessage(
											owner,
											Economy.INFO
													+ "The winner of your auction is not online.");
							if (owner != null) {
								owner.giveItem(Auction.this.itemId,
										Auction.this.amount);
								Auction.this.economy
										.removeAuction(Auction.this.owner);
							}
							return;
						}
						Economy.sendAll(Economy.INFO + "And the winner of "
								+ Colors.White + Auction.this.amount + " "
								+ Economy.itemName(Auction.this.itemId)
								+ Economy.INFO + " is " + Economy.ONLINE
								+ Auction.this.highestBidder + Economy.INFO
								+ " for " + Economy.MONEY
								+ Economy.format(Auction.this.currentBid)
								+ Economy.INFO + "! Congratulations!");
						Player winner = etc.getServer().getPlayer(
								Auction.this.highestBidder);
						Auction.this.economy.addToBalance(Auction.this.owner,
								Auction.this.currentBid);
						Auction.this.economy.addToBalance(
								Auction.this.highestBidder,
								-Auction.this.currentBid);
						winner.giveItem(Auction.this.itemId,
								Auction.this.amount);
						Economy.sendMessage(winner, Economy.INFO
								+ "You have won the auction!");
						Economy.sendMessage(winner, Economy.INFO
								+ "Your balance has decreased to "
								+ Economy.MONEY
								+ Economy.format(Auction.this.economy
										.getBalance(winner.getName()))
								+ Economy.INFO + ".");
						if (owner != null) {
							Economy.sendMessage(owner, Economy.INFO
									+ "Your balance has increased to "
									+ Economy.MONEY
									+ Economy.format(Auction.this.economy
											.getBalance(Auction.this.owner))
									+ Economy.INFO + ".");
						}
						Auction.this.economy.removeAuction(Auction.this.owner);
					} catch (Exception e) {
						e.printStackTrace();
						Auction.this.running = false;
						Auction.this.finished = true;
					}
				}
			}).start();
		}

		/**
		 * Gets the time remaining in this auction.
		 * 
		 * @return the time remaining.
		 */
		public String timeRemaining() {
			if (!this.running) {
				return "N/A";
			}
			return Economy
					.formatTime((this.startTime + 1000.0 * this.auctionLength - System
							.currentTimeMillis()) / 1000.0 / 60.0);
		}
	}

	/**
	 * Returns a page of a list.
	 * 
	 * @param player
	 *            the player displaying the list.
	 * @param list
	 *            the list to display.
	 * @param page
	 *            the page number to display.
	 * @return a single page of the specified list.
	 */
	private static final <T> List<T> getPage(Player player, List<T> list,
			int page) {
		int maxPage = (list.size() - 1) / RESULTS_PER_PAGE + 1;
		if (page < 1) {
			Economy.sendMessage(player, Economy.ERROR
					+ "Warning - page must be between 1 and " + maxPage + ".");
			page = 1;
		} else if (page > maxPage) {
			Economy.sendMessage(player, Economy.ERROR
					+ "Warning - page must be between 1 and " + maxPage + ".");
			page = maxPage;
		}

		Economy.sendMessage(player, Economy.INFO + "Page " + Colors.White
				+ page + Economy.INFO + " of " + Colors.White + maxPage
				+ Economy.INFO + ":");
		return list.subList((page - 1) * RESULTS_PER_PAGE, Math.min(page
				* RESULTS_PER_PAGE, list.size()));
	}

	/**
	 * Saves the state of the economy.
	 */
	public void save() {
		this.writeEconomy();
		this.writeLocations();
		this.writeAccounts();
	}

	/**
	 * Gets a list of the player's offers.
	 * 
	 * @param player
	 *            the player.
	 * @return a list of their offers.
	 */
	public final ArrayList<Offer> getOffers(Player player) {
		return this.getOffers(player.getName());
	}

	/**
	 * Gets a list of the player's offers.
	 * 
	 * @param playerName
	 *            the player.
	 * @return a list of their offers.
	 */
	public final ArrayList<Offer> getOffers(String playerName) {
		ArrayList<Offer> result = this.sellingOffers.get(playerName
				.toLowerCase());
		if (result == null) {
			result = new ArrayList<Offer>();
			this.sellingOffers.put(playerName.toLowerCase(), result);
		}
		return result;
	}

	private class EconomyListener extends PluginListener {
		private final Economy economy;

		public EconomyListener(Economy economy) {
			this.economy = economy;
		}

		@Override
		public boolean onSignChange(Player player, Sign sign) {
			if (sign.getText(0).trim().equalsIgnoreCase("[sell]")) {
				if (!this.offerAdd(player, sign)) {
					etc.getServer().setBlockAt(0, sign.getX(), sign.getY(),
							sign.getZ());
					player.giveItem(Item.Type.Sign.getId(), 1);
				}
			}
			return false;
		}

		@Override
		public void onLogin(Player player) {
			if (this.economy.balances.isEmpty() && player.isAdmin()) {
				Economy.sendMessage(player, Economy.INFO
						+ "Thank you for choosing The New Economy.");
				Economy.sendMessage(player, Economy.INFO + "Here's "
						+ Economy.MONEY + Economy.format(100.0) + Economy.INFO
						+ " to get you started.");
				this.economy
						.addToBalance(player.getName().toLowerCase(), 100.0);
				return;
			} else if (this.economy.balances.containsKey(player.getName()
					.toLowerCase())
					|| !player.canUseCommand("/balance")) {
				return;
			}
			if (Economy.STARTING_MONEY > 0
					&& this.economy.publicFund >= Economy.STARTING_MONEY) {
				Economy.sendMessage(player, Economy.INFO
						+ "Welcome to the sever.");
				Economy.sendMessage(player, Economy.INFO + "Here's "
						+ Economy.MONEY
						+ Economy.format(Economy.STARTING_MONEY) + Economy.INFO
						+ " to get you started.");
				this.economy.publicFund -= Economy.STARTING_MONEY;
				this.economy.addToBalance(player.getName(),
						Economy.STARTING_MONEY);
			}
		}

		@Override
		public boolean onMobSpawn(Mob mob) {
			return this.economy.getArea(mob.getX(), mob.getY(), mob.getZ()) != null;
		}

		@Override
		public boolean onBlockPlace(Player player, Block block,
				Block blockClicked, Item itemInHand) {
			Area area = this.economy.getArea(block.getX(), block.getY(), block
					.getZ());
			if (area == null) {
				return false;
			}
			if (!area.owner.equalsIgnoreCase(player.getName())) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Can't place blocks here. This area belongs to "
						+ (Economy.online(area.owner) ? Economy.ONLINE
								: Economy.OFFLINE) + area.owner + Economy.ERROR
						+ ".");
				return true;
			}
			return false;
		}

		@Override
		public boolean onBlockBreak(Player player, Block block) {
			if (block.getType() == Block.Type.SignPost.getType()
					|| block.getType() == Block.Type.WallSign.getType()) {
				Sign sign = (Sign) etc.getServer().getComplexBlock(block);
				if (sign.getText(0).trim().equalsIgnoreCase("[sell]")) {
					Offer foundOffer = null;
					for (Offer offer : this.economy.allOffers) {
						Sign s = offer.sellingSign();
						if (s != null && s.hashCode() == sign.hashCode()) {
							foundOffer = offer;
							break;
						}
					}
					if (foundOffer != null) {
						if (foundOffer.owner.equalsIgnoreCase(player.getName())) {
							this.offerRemove(player,
									("/offer remove id:" + foundOffer.offerId)
											.split(" "));
							return false;
						} else {
							Economy.sendMessage(player, Economy.ERROR
									+ "Error - you can't remove "
									+ foundOffer.owner + "\'s offer sign.");
							// TODO fix sign not saving text after broken
							return true;
						}
					}
				}
			}
			Area area = this.economy.getArea(block.getX(), block.getY(), block
					.getZ());
			if (area == null) {
				return false;
			}
			if (!(area instanceof PersonalArea)) {
				return false;
			}
			if (!area.owner.equalsIgnoreCase(player.getName())) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Can't remove blocks here. This area belongs to "
						+ (Economy.online(area.owner) ? Economy.ONLINE
								: Economy.OFFLINE) + area.owner + Economy.ERROR
						+ ".");
				return true;
			}
			return false;
		}

		@Override
		public boolean onCommand(Player player, String[] split) {
			// TODO shorthand command for "next page"
			if (!this.economy.isEnabled()) {
				return false;
			}
			String command = split[0].toLowerCase();

			if (command.equals("/buy")) {
				if (!player.canUseCommand("/buy")) {
					return false;
				}
				this.buy(player, split);
				return true;
			} else if (command.equals("/saveeconomy")) {
				if (!player.canUseCommand("/saveEconomy")) {
					return false;
				}
				this.saveEconomy(player, split);
				return true;
			} else if (command.equals("/area")) {
				if (!player.canUseCommand("/area")) {
					return false;
				}
				if (split.length <= 1) {
					Economy
							.sendMessage(
									player,
									Economy.COMMAND
											+ "Usage: /area <add | get | give | list | name | remove>");
					return true;
				}
				String subcommand = split[1].toLowerCase();
				if (subcommand.equals("add") || subcommand.equals("-a")) {
					this.areaAdd(player, split);
				} else if (subcommand.equals("get")) {
					this.areaGet(player, split);
				} else if (subcommand.equals("give")) {
					this.areaGive(player, split);
				} else if (subcommand.equals("list") || subcommand.equals("-l")) {
					this.areaList(player, split);
				} else if (subcommand.equals("name") || subcommand.equals("-n")) {
					this.areaName(player, split);
				} else if (subcommand.equals("remove")
						|| subcommand.equals("-r")) {
					this.areaRemove(player, split);
				} else {
					Economy
							.sendMessage(
									player,
									Economy.COMMAND
											+ "Usage: /area <add | get | give | list | name | remove>");
				}
				return true;
			} else if (command.equals("/offer")) {
				if (!player.canUseCommand("/offer")) {
					return false;
				}
				if (split.length <= 1) {
					Economy.sendMessage(player, Economy.COMMAND
							+ "Usage: /offer <add | list | listen | remove>");
					return true;
				}
				String subcommand = split[1].toLowerCase();
				if (subcommand.equals("add") || subcommand.equals("-a")) {
					this.offerAdd(player, split);
				} else if (subcommand.equals("list")) {
					this.offerList(player, split);
				} else if (subcommand.equals("remove")
						|| subcommand.equals("-r")) {
					this.offerRemove(player, split);
				} else if (subcommand.equals("listen")) {
					this.offerListen(player, split);
				} else {
					Economy.sendMessage(player, Economy.COMMAND
							+ "Usage: /offer <add | list | listen | remove>");
				}
				return true;
			} else if (command.equals("/money")) {
				if (!player.canUseCommand("/money")) {
					return false;
				}
				if (split.length <= 1) {
					Economy
							.sendMessage(
									player,
									Economy.COMMAND
											+ "Usage: /money <balance | deposit | give | lottery | pay | withdraw>");
					return true;
				}
				String subcommand = split[1].toLowerCase();
				if (subcommand.equals("balance") || subcommand.equals("-b")) {
					this.moneyBalance(player, split);
				} else if (subcommand.equals("deposit")
						|| subcommand.equals("-d")) {
					this.moneyDeposit(player, split);
				} else if (subcommand.equals("give") || subcommand.equals("-g")) {
					this.moneyGive(player, split);
				} else if (subcommand.equals("pay") || subcommand.equals("-p")) {
					this.moneyPay(player, split);
				} else if (subcommand.equals("withdraw")
						|| subcommand.equals("-w")) {
					this.moneyWithdraw(player, split);
				} else if ((subcommand.equals("lottery") || subcommand
						.equals("-l"))
						&& player.isAdmin()) {
					this.moneyLottery(player, split);
				} else {
					Economy
							.sendMessage(
									player,
									Economy.COMMAND
											+ "Usage: /money <balance | deposit | give | lottery | pay | withdraw>");
				}
				return true;
			} else if (command.equals("/auction")) {
				if (!player.canUseCommand("/auction")) {
					return false;
				}
				if (split.length <= 1) {
					Economy
							.sendMessage(
									player,
									Economy.COMMAND
											+ "Usage: /auction <add | bid | list | remove | start>");
					return true;
				}
				String subcommand = split[1].toLowerCase();
				if (subcommand.equals("add") || subcommand.equals("-a")) {
					this.auctionAdd(player, split);
				} else if (subcommand.equals("remove")
						|| subcommand.equals("-r")) {
					this.auctionRemove(player, split);
				} else if (subcommand.equals("bid") || subcommand.equals("-b")) {
					this.auctionBid(player, split);
				} else if (subcommand.equals("list") || subcommand.equals("-l")) {
					this.auctionList(player, split);
				} else if (subcommand.equals("start")
						|| subcommand.equals("-s")) {
					this.auctionStart(player, split);
				} else {
					Economy
							.sendMessage(
									player,
									Economy.COMMAND
											+ "Usage: /auction <add | bid | list | remove | start>");
				}
				return true;
			} else if (command.equals("/bank")) {
				if (!player.canUseCommand("/bank")) {
					return false;
				}
				if (split.length <= 1) {
					Economy.sendMessage(player, Economy.COMMAND
							+ "Usage: /bank <contents | deposit | withdraw>");
					return true;
				}
				String subcommand = split[1].toLowerCase();
				if (subcommand.equals("contents") || subcommand.equals("-c")) {
					this.bankContents(player, split);
				} else if (subcommand.equals("deposit")
						|| subcommand.equals("-d")) {
					this.bankDeposit(player, split);
				} else if (subcommand.equals("withdraw")
						|| subcommand.equals("-w")) {
					this.bankWithdraw(player, split);
				} else {
					Economy.sendMessage(player, Economy.COMMAND
							+ "Usage: /bank <contents | deposit | withdraw>");
				}
				return true;
			}
			return false;
		}

		/**
		 * Withdraws items from the player's back account.
		 * 
		 * @param player
		 *            the player.
		 * @param split
		 *            parameters.
		 */
		private final void bankWithdraw(Player player, String[] split) {
			if (split.length < 3 || split[2].equals("?")) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /bank <withdraw|-w> <item:itemName> <amount:itemAmount|\'all\'>");
				return;
			}

			Area currentArea = this.economy.getArea(player);
			if (currentArea == null || !(currentArea instanceof BankArea)) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - you must be in a banking area to make a withdrawal.");
				if (player.canUseCommand("/area")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Try using the \"/area list\" command.");
				}
				return;
			}

			HashMap<String, String> kv = Economy.toKV(split);

			String itemName = null;
			if (kv.containsKey("item")) {
				itemName = kv.get("item");
			} else if (split.length > 2) {
				itemName = split[2];
			}

			if (itemName == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you must specify an item name.");
				return;
			}

			int itemId = Economy.itemId(itemName);
			if (itemId == -1) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - could not find item id for " + itemName
						+ ".");
				return;
			}

			String amountString = null;
			if (kv.containsKey("amount")) {
				amountString = kv.get("amount");
			} else if (split.length > 3) {
				amountString = split[3];
			}

			if (amountString == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you must specify an amount.");
				return;
			}

			int amount;
			try {
				amount = Integer.parseInt(amountString);
				if (amount <= 0) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you must withdraw a positive amount.");
					return;
				}
			} catch (Exception e) {
				if (!amountString.equals("all")) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - amount must be a valid number.");
					return;
				}
				amount = -1;
			}

			TreeSet<BankSlot> account = this.economy.getAccount(player);
			BankSlot found = null;
			for (BankSlot slot : account) {
				if (slot.itemId == itemId) {
					found = slot;
					break;
				}
			}

			if (found == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you don't have any " + itemName
						+ " in your account.");
				return;
			}

			if (amount < 0) {
				amount = found.amount;
			}

			int givePlayer;
			if (found.amount < amount) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Warning - can only withdraw " + found.amount + " "
						+ itemName + ".");
				givePlayer = found.amount;
			} else {
				givePlayer = amount;
			}
			player.giveItem(itemId, givePlayer);
			found.amount -= givePlayer;
			if (found.amount <= 0) {
				account.remove(found);
			}
			if (givePlayer > 0) {
				Economy.sendMessage(player, Economy.INFO
						+ "You have withdrawn " + Colors.White + givePlayer
						+ " " + itemName + Economy.INFO + ".");
			} else {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - could not withdraw any " + itemName + ".");
			}
		}

		/**
		 * Deposits items in the player's bank account.
		 * 
		 * @param player
		 *            the player.
		 * @param split
		 *            parameters.
		 */
		private final void bankDeposit(Player player, String[] split) {
			if (split.length < 3 || split[2].equals("?")) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /bank <deposit|-d> <item:itemName> <amount:itemAmount|\'all\'>");
				return;
			}

			Area currentArea = this.economy.getArea(player);
			if (currentArea == null || !(currentArea instanceof BankArea)) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - you must be in a bank area to make a deposit.");
				if (player.canUseCommand("/area")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Try using the \"/area list\" command.");
				}
				return;
			}

			HashMap<String, String> kv = Economy.toKV(split);

			String itemName = null;
			if (kv.containsKey("item")) {
				itemName = kv.get("item");
			} else if (split.length > 2) {
				itemName = split[2];
			}

			if (itemName == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you must specify an item name.");
				return;
			}

			int itemId = Economy.itemId(itemName);
			if (itemId == -1) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - could not find item id for " + itemName
						+ ".");
				return;
			}

			int count = Economy.countItem(player, itemId);

			if (count <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you don't have any " + itemName + ".");
				return;
			}

			String amountString = null;
			if (kv.containsKey("amount")) {
				amountString = kv.get("amount");
			} else if (split.length > 3) {
				amountString = split[3];
			}

			if (amountString == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you must specify an amount.");
				return;
			}

			int amount;
			try {
				amount = Integer.parseInt(amountString);
			} catch (Exception e) {
				if (!amountString.equalsIgnoreCase("all")) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - amount must be a valid integer.");
					return;
				}
				amount = count;
			}

			if (count < amount) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you do not have enough " + itemName + ".");
				return;
			}

			BankSlot found = null;
			TreeSet<BankSlot> account = this.economy.getAccount(player);
			for (BankSlot slot : account) {
				if (slot.itemId == itemId) {
					found = slot;
					break;
				}
			}
			Economy.sendMessage(player, Economy.INFO + "You have deposited "
					+ Colors.White + amount + " " + itemName + Economy.INFO
					+ ".");
			if (found == null) {
				account.add(new BankSlot(itemId, amount));
			} else {
				found.amount += amount;
				Economy.sendMessage(player, Economy.INFO + "Now you have "
						+ Colors.White + found.amount + " " + itemName
						+ Economy.INFO + " in your account.");
			}
			player.getInventory().removeItem(itemId, amount);
			player.getInventory().update();
		}

		/**
		 * Lists the contents of the player's bank account.
		 * 
		 * @param player
		 *            the player.
		 * @param split
		 *            parameters (unused).
		 */
		private final void bankContents(Player player, String[] split) {
			Area area = this.economy.getArea(player);
			if (area == null || !(area instanceof BankArea)) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - you must be in a bank to check your bank account.");
				return;
			}

			TreeSet<BankSlot> account = this.economy.getAccount(player);

			if (account.size() == 0) {
				Economy.sendMessage(player, Economy.INFO
						+ "You currently have no items in your bank account.");
				return;
			}

			StringBuilder message = new StringBuilder();
			message.append(Economy.INFO + "Your account: ");
			Iterator<BankSlot> iterator = account.iterator();
			while (iterator.hasNext()) {
				BankSlot slot = iterator.next();
				message.append(Colors.White + slot.amount + " "
						+ Economy.itemName(slot.itemId));
				if (iterator.hasNext()) {
					message.append(Economy.INFO + ", ");
				}
			}
			Economy.sendMessage(player, message.toString());
		}

		/**
		 * Starts a player's auction.
		 * 
		 * @param player
		 *            the player starting their auction.
		 * @param split
		 *            parameters (unused)
		 */
		private final void auctionStart(Player player, String[] split) {
			Auction auction = this.economy.auctions.get(player.getName()
					.toLowerCase());
			if (auction == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you don't have an auction currently.");
				return;
			}

			if (auction.running) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - your auction is already running.");
				return;
			} else if (auction.finished) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - your auction has already finished.");
				Economy.sendMessage(player, Economy.INFO
						+ "Remove your auction with \"/auction remove\".");
				return;
			}

			auction.start();
		}

		/**
		 * Adds an auction. The player specifies the item name, the amount, the
		 * starting bid, and optionally the length of the auction in seconds.
		 * 
		 * @param player
		 *            the player adding an auction.
		 * @param split
		 *            parameters.
		 */
		private final void auctionAdd(Player player, String[] split) {
			if (split.length < 3 || split[3].equals("?")) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /auction <add|-a> <item:itemName> <amount:itemAmount|\'all\'> <start:startingBid> [time:length]");
				return;
			}

			String playerName = player.getName().toLowerCase();
			if (this.economy.auctions.containsKey(playerName)) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you already have an auction.");
				return;
			}

			Area currentArea = this.economy.getArea(player);
			if (currentArea == null || !(currentArea instanceof TradeArea)) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - you must be in a trading area to add an auction.");
				if (player.canUseCommand("/area")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Try using the \"/area list\" command.");
				}
				return;
			}

			final HashMap<String, String> kv = Economy.toKV(split);

			String itemName = null;
			if (kv.containsKey("item")) {
				itemName = kv.get("item");
			} else if (split.length > 2) {
				itemName = split[2];
			}
			if (itemName == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - must specify an item name.");
				return;
			}

			int itemId = Economy.itemId(itemName);
			if (itemId == -1) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - could not find item id for "
						+ itemName.toLowerCase() + ".");
				return;
			}

			int totalAmount = Economy.countItem(player, itemId);

			if (totalAmount <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you do not have any " + itemName + ".");
				return;
			}

			String amountString = null;
			if (kv.containsKey("amount")) {
				amountString = kv.get("amount");
			} else if (split.length > 3) {
				amountString = split[3];
			}
			if (amountString == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - must specify an item amount.");
				return;
			}

			int amount;
			try {
				amount = Integer.parseInt(amountString);
			} catch (Exception e) {
				if (!amountString.equalsIgnoreCase("all")) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - amount must be an integer.");
					return;
				}
				amount = totalAmount;
			}

			if (amount <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - amount must be positive.");
				return;
			}

			if (totalAmount < amount) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you do not have enough " + itemName + ".");
				return;
			}

			String startingString = null;
			if (kv.containsKey("start")) {
				startingString = kv.get("start");
			} else if (split.length > 4) {
				startingString = split[4];
			}
			if (startingString == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - must specify a starting bid.");
				return;
			}

			double startingBid;
			try {
				startingBid = Double.parseDouble(startingString);
				startingBid = Math.round(startingBid * 100.0) / 100.0;
			} catch (Exception e) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - starting bid must be a valid number.");
				return;
			}

			if (startingBid < 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - starting bid must be at least "
						+ Economy.MONEY + "0.00" + Economy.ERROR + ".");
			}

			Auction auction = new Auction(this.economy, player.getName(),
					itemId, amount, startingBid);

			String timeString = null;
			if (kv.containsKey("time")) {
				timeString = kv.get("time");
			} else if (split.length > 5) {
				timeString = split[5];
			}
			if (timeString != null) {
				double length;
				try {
					length = Double.parseDouble(timeString);

				} catch (Exception e) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - auction length must be a valid number.");
					return;
				}

				if (length < Economy.MIN_AUCTION_LENGTH) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Warning - auction length must be at least "
							+ Economy.MIN_AUCTION_LENGTH + ".");
					length = Economy.MIN_AUCTION_LENGTH;
				} else if (length > Economy.MAX_AUCTION_LENGTH) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Warning - auction length can be at most "
							+ Economy.MAX_AUCTION_LENGTH + ".");
					length = Economy.MAX_AUCTION_LENGTH;
				}

				auction.auctionLength = length;
			}

			player.getInventory().removeItem(itemId, amount);
			player.getInventory().update();
			this.economy.auctions.put(player.getName().toLowerCase(), auction);
			Economy
					.sendMessage(
							player,
							Economy.INFO
									+ "Your auction has been created. Start your auction with \"/auction start\".");
		}

		/**
		 * Removes the player's auction if they have one.
		 * 
		 * @param player
		 *            the player.
		 * @param split
		 *            parameters (unused).
		 */
		private final void auctionRemove(Player player, String[] split) {
			if (!this.economy.auctions.containsKey(player.getName()
					.toLowerCase())) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you don't have an auction currently.");
				return;
			}
			String playerName = player.getName().toLowerCase();
			Auction auction = this.economy.auctions.get(playerName);

			player.giveItem(auction.itemId, auction.amount);
			this.economy.auctions.remove(playerName);
			if (auction.running) {
				Economy.sendAll(Economy.ONLINE + playerName + "\'s"
						+ Economy.INFO + " auction has been removed.");
			} else {
				Economy.sendMessage(player, Economy.INFO
						+ "Your auction has been removed.");
			}
			auction.running = false;
			auction.finished = true;
		}

		private final void auctionBid(Player player, String[] split) {
			if (split.length < 3 || split[3].equals("?")) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /auction <bid|-b> <seller:playerName | id:auctionId> <bid:amount>");
				return;
			}

			Area currentArea = this.economy.getArea(player);
			if (currentArea == null || !(currentArea instanceof TradeArea)) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - you must be in a trading area to make a bid.");
				if (player.canUseCommand("/area")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Try using the \"/area list\" command.");
				}
				return;
			}

			final HashMap<String, String> kv = Economy.toKV(split);
			Auction biddingOn = null;

			String seller = null;
			if (kv.containsKey("seller")) {
				seller = kv.get("seller");
			} else if (split.length > 2) {
				seller = split[2];
			}

			String idString = null;
			if (kv.containsKey("id")) {
				idString = kv.get("id");
			} else if (split.length > 2) {
				idString = split[2];
			}

			if (idString == null && seller == null) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - must specify either a seller or auction id.");
				return;
			}

			if (seller != null) {
				if (player.getName().equalsIgnoreCase(seller)) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you can't bid on your own auction.");
					return;
				}
				biddingOn = this.economy.auctions.get(seller);
			}

			if (idString != null && biddingOn == null) {
				int auctionId;
				try {
					auctionId = Integer.parseInt(idString);
				} catch (Exception e) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - auction id must be a valid number.");
					return;
				}
				for (Auction auction : this.economy.auctions.values()) {
					if (auction.auctionId == auctionId) {
						biddingOn = auction;
						break;
					}
				}
			}

			if (biddingOn == null) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - couldn't find the auction you're looking for.");
				return;
			}

			if (!biddingOn.running) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - that auction is not running.");
				return;
			}

			double bid;
			try {
				if (kv.containsKey("bid")) {
					bid = Double.parseDouble(kv.get("bid"));
				} else if (split.length > 3) {
					bid = Double.parseDouble(split[3]);
				} else {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you must specify a bid.");
					return;
				}
			} catch (Exception e) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - your bid must be a valid number.");
				return;
			}

			if (bid <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you must bid a positive amount.");
				return;
			}

			if (this.economy.getBalance(player.getName()) < bid) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - insufficient funds.");
				Economy.sendMessage(player, Economy.INFO
						+ "Your current balance is "
						+ Economy.MONEY
						+ Economy.format(this.economy.getBalance(player
								.getName())) + Economy.INFO + ".");
				return;
			}

			if (bid <= biddingOn.currentBid) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you must bid at least " + Economy.MONEY
						+ Economy.format(biddingOn.currentBid) + Economy.ERROR
						+ ".");
				return;
			}

			biddingOn.highestBidder = player.getName().toLowerCase();
			biddingOn.currentBid = bid;

			Economy.sendAll(Economy.ONLINE + player.getName() + Economy.INFO
					+ " is in the lead for " + Colors.White + biddingOn.amount
					+ " " + Economy.itemName(biddingOn.itemId) + Economy.INFO
					+ " with a bid of " + Economy.MONEY + Economy.format(bid)
					+ Economy.INFO + "! There is " + Colors.White
					+ biddingOn.timeRemaining() + Economy.INFO
					+ " remaining! [" + Colors.White + biddingOn.auctionId
					+ Economy.INFO + "]");
		}

		private final void auctionList(final Player player, final String[] split) {
			if (split.length > 2 && split[2].equals("?")) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /auction <list|-l> [seller:playerName] [item:itemName] [page:pageNumber]");
				return;
			}

			final HashMap<String, String> kv = Economy.toKV(split);

			int page = 1;
			try {
				if (kv.containsKey("page")) {
					page = Integer.parseInt(kv.get("page"));
				} else if (split.length > 4) {
					page = Integer.parseInt(split[4]);
				}
			} catch (Exception e) {
				Economy.sendMessage(player, ERROR
						+ "Error - page must be a valid number.");
				return;
			}

			Condition<Auction> displayCondition = new Condition<Auction>() {
				@Override
				public boolean isValid(Auction auction) {
					boolean result = true;
					String seller = null;
					if (kv.containsKey("seller")) {
						seller = kv.get("seller");
					} else if (split.length > 2) {
						seller = split[2];
					}
					if (seller != null) {
						result &= auction.owner.equalsIgnoreCase(kv
								.get("seller"));
					}
					String itemName = null;
					if (kv.containsKey("item")) {
						itemName = kv.get("item");
					} else if (split.length > 3) {
						itemName = split[3];
					}

					if (itemName != null) {
						int itemId = Economy.itemId(itemName);
						if (itemId == -1) {
							Economy.sendMessage(player, Economy.ERROR
									+ "Warning - could not find item id for "
									+ kv.get("item"));
						} else {
							result &= auction.itemId == itemId;
						}
					}
					return result;
				}
			};

			ArrayList<Auction> validAuctions = new ArrayList<Auction>();
			for (Auction auction : this.economy.auctions.values()) {
				if (displayCondition.isValid(auction)) {
					validAuctions.add(auction);
				}
			}

			if (validAuctions.size() == 0) {
				Economy.sendMessage(player, Economy.INFO
						+ "No auctions to display.");
				return;
			}

			Economy.sendMessage(player, Economy.INFO + "Current auctions:");
			for (Auction auction : getPage(player, validAuctions, page)) {
				StringBuilder message = new StringBuilder();
				message.append((Economy.online(auction.owner) ? Economy.ONLINE
						: Economy.OFFLINE)
						+ auction.owner);
				message.append(Economy.INFO + ": ");
				message.append(Colors.White + auction.amount + " "
						+ Economy.itemName(auction.itemId));
				message.append(Economy.INFO + " - ");
				message.append(Economy.MONEY
						+ Economy.format(auction.currentBid));
				if (auction.highestBidder != null) {
					message.append(Economy.INFO + " (");
					message
							.append((Economy.online(auction.highestBidder) ? Economy.ONLINE
									: Economy.OFFLINE)
									+ auction.highestBidder);
					message.append(Economy.INFO + ")");
				}
				message.append(Economy.INFO + " with ");
				message.append(Colors.White + auction.timeRemaining());
				message.append(Economy.INFO + " remaining. [");
				message.append(Colors.White + auction.auctionId);
				message.append(Economy.INFO + "]");
				Economy.sendMessage(player, message.toString());
			}
		}

		private final void moneyWithdraw(Player player, String[] split) {
			if (split.length < 3) {
				Economy.sendMessage(player, Economy.COMMAND
						+ "Usage: /money <withdraw|-w> <amount | \'all\' >");
				return;
			}

			Area area = this.economy.getArea(player);
			if (area == null || !(area instanceof BankArea)) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - you must be in a banking area to withdraw money.");
				if (player.canUseCommand("/area")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Try using the \"/area list\" command.");
				}
				return;
			}

			double withdrawAmount;
			try {
				withdrawAmount = Double.parseDouble(split[2]);
				withdrawAmount = Math.round(withdrawAmount * 100.0) / 100.0;
			} catch (Exception e) {
				if (!split[2].equalsIgnoreCase("all")) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - amount must be a valid number.");
					return;
				}
				withdrawAmount = this.economy.getBalance(player.getName());
			}

			if (withdrawAmount <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - must withdraw a positive amount.");
				return;
			}

			if (this.economy.getBalance(player.getName()) < withdrawAmount) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - insufficient funds.");
				Economy.sendMessage(player, Economy.INFO
						+ "Your current balance is "
						+ Economy.MONEY
						+ Economy.format(this.economy.getBalance(player
								.getName())) + Economy.INFO + ".");
				return;
			}

			TreeMap<Double, Integer> VALUE_ITEMS = new TreeMap<Double, Integer>(
					new Comparator<Double>() {
						@Override
						public int compare(Double arg0, Double arg1) {
							if (arg0 < arg1) {
								return 1;
							} else if (arg0 == arg1) {
								return 0;
							} else {
								return -1;
							}
						}
					}) {
				{
					for (Map.Entry<Integer, Double> entry : Economy.ITEM_VALUES
							.entrySet()) {
						this.put(entry.getValue(), entry.getKey());
					}
				}
			};

			boolean removed = false;
			double totalWithdraw = 0.0;
			for (Map.Entry<Double, Integer> entry : VALUE_ITEMS.entrySet()) {
				double itemValue = entry.getKey();
				int itemId = entry.getValue();
				int count = (int) (withdrawAmount / itemValue);
				if (count == 0) {
					continue;
				}
				player.giveItem(itemId, count);
				withdrawAmount -= count * itemValue;
				totalWithdraw -= count * itemValue;
				removed = true;
			}
			if (withdrawAmount > 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Warning - could not withdraw " + Economy.MONEY
						+ Economy.format(withdrawAmount) + Economy.ERROR + ".");
			}
			if (removed) {
				this.economy.addToBalance(player.getName(), totalWithdraw);
				Economy.sendMessage(player, Economy.INFO
						+ "Your balance has decreased to "
						+ Economy.MONEY
						+ Economy.format(this.economy.getBalance(player
								.getName())) + Economy.INFO + ".");
			}
		}

		/**
		 * Attempts to run a lottery. Lotteries can only be run once every 60
		 * minutes by default. This can be changed in economy.properties.
		 * 
		 * @param player
		 *            the player running the lottery.
		 * @param split
		 *            parameters (unused).
		 */
		private final void moneyLottery(Player player, String[] split) {
			final long currentTime = System.currentTimeMillis();
			double lotteryMs = Economy.LOTTERY_INTERVAL * 60.0 * 1000.0;
			if (currentTime - this.economy.lastLottery < lotteryMs) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - lottery has been run too recently.");
				Economy.sendMessage(player, Economy.ERROR
						+ "You must wait another "
						+ Colors.White
						+ Economy.formatTime(Economy.LOTTERY_INTERVAL
								- (currentTime - this.economy.lastLottery)
								/ 1000.0 / 60.0) + Economy.ERROR
						+ " before trying again.");
				return;
			}

			this.economy.lastLottery = currentTime;

			List<Player> players = etc.getServer().getPlayerList();
			Iterator<Player> iterator = players.iterator();
			while (iterator.hasNext()) {
				if (!iterator.next().canUseCommand("/money")) {
					iterator.remove();
				}
			}

			if (players.size() <= 1) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - there must be more people online to have a lottery.");
				return;
			}

			double maxWinnings = this.economy.publicFund
					* (1.0 - Math.exp(Math.log(2)
							/ (Economy.PLAYERS_TO_HALF - 1)
							* (1 - players.size())));
			maxWinnings = Math.round(100.0 * maxWinnings) / 100.0;

			Player winner = players.get((int) (Math.random() * players.size()));

			for (Player p : players) {
				Economy.sendMessage(p, Economy.ONLINE + player.getName()
						+ Economy.INFO + " has started the lottery!");
				Economy.sendMessage(p, Economy.MONEY
						+ Economy.format(maxWinnings) + Economy.INFO
						+ " is up for stake!");
				Economy.sendMessage(p, Economy.INFO + "And the winner is... "
						+ Economy.ONLINE + winner.getName() + Economy.INFO
						+ "! Congratulations!");
			}

			this.economy.addToBalance(winner.getName(), maxWinnings);
			this.economy.publicFund -= maxWinnings;

			Economy.sendMessage(winner, Economy.INFO
					+ "Your balance has increased to " + Economy.MONEY
					+ Economy.format(this.economy.getBalance(winner.getName()))
					+ Economy.INFO + ".");
		}

		private final void offerListen(Player player, String[] split) {
			String playerName = player.getName().toLowerCase();
			if (split.length < 3) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /offer listen <itemName1|\'none\'|?> [itemName2] ... [itemNameN]");
				return;
			}

			if (split[2].equalsIgnoreCase("none")) {
				boolean found = false;
				for (HashSet<String> listeners : this.economy.offerListeners
						.values()) {
					if (listeners.contains(playerName)) {
						found = true;
					}
					listeners.remove(playerName);
				}
				if (found) {
					Economy.sendMessage(player, Economy.INFO
							+ "You are no longer listening for any items.");
				} else {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you are not listening for any items.");
				}
				return;
			} else if (split[2].equals("?")) {
				HashSet<Integer> listeningFor = new HashSet<Integer>();
				for (Map.Entry<Integer, HashSet<String>> entry : this.economy.offerListeners
						.entrySet()) {
					if (entry.getValue().contains(
							player.getName().toLowerCase())) {
						listeningFor.add(entry.getKey());
					}
				}
				if (listeningFor.size() == 0) {
					Economy.sendMessage(player, Economy.INFO
							+ "You are not listening for any items.");
				} else {
					StringBuilder message = new StringBuilder();
					message.append(Economy.INFO);
					message.append("You are listening for ");
					int listenerIndex = 0;
					for (Integer id : listeningFor) {
						message.append(Economy.itemName(id));
						if (listenerIndex < listeningFor.size() - 1) {
							if (listeningFor.size() > 2) {
								message.append(',');
							}
							message.append(' ');
						}
						if (listenerIndex == listeningFor.size() - 2) {
							message.append("and ");
						}
						++listenerIndex;
					}
					message.append(".");
					Economy.sendMessage(player, message.toString());
				}
				return;
			}

			for (int itemIndex = 2; itemIndex < split.length; ++itemIndex) {
				int itemId = Economy.itemId(split[itemIndex]);
				if (itemId == -1) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - could not find item id for "
							+ split[itemIndex] + ".");
					continue;
				}

				HashSet<String> listeners = this.economy.offerListeners
						.get(itemId);
				if (listeners == null) {
					listeners = new HashSet<String>();
					this.economy.offerListeners.put(itemId, listeners);
				}

				if (listeners.contains(playerName)) {
					Economy.sendMessage(player, Economy.INFO
							+ "You are no longer listening for "
							+ split[itemIndex] + ".");
					listeners.remove(playerName);
					return;
				}

				listeners.add(playerName);
				Economy
						.sendMessage(player, Economy.INFO
								+ "You are now listening for "
								+ split[itemIndex] + ".");
			}
		}

		/**
		 * Transfers an area from the current player to another.
		 * 
		 * @param player
		 *            the player.
		 * @param split
		 *            parameters.
		 */
		private final void areaGive(Player player, String[] split) {
			if (split.length < 4) {
				Economy.sendMessage(player, Economy.COMMAND
						+ "Usage: /area give <id:areaId> <to:playerName>");
				return;
			}

			HashMap<String, String> kv = Economy.toKV(split);

			String areaIdString = kv.get("id");
			int areaId;
			try {
				if (areaIdString == null) {
					areaId = Integer.parseInt(split[2]);
				} else {
					areaId = Integer.parseInt(areaIdString);
				}
			} catch (Exception e) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - area id must be a valid number.");
				return;
			}

			String giveTo = kv.get("to");
			if (giveTo == null) {
				giveTo = split[3];
			}

			if (player.getName().equalsIgnoreCase(giveTo)) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you can't transfer an area to yourself.");
				return;
			}

			Player otherPlayer = etc.getServer().getPlayer(giveTo);
			if (otherPlayer == null
					&& !this.economy.balances.containsKey(giveTo)) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - can't find player " + Economy.OFFLINE
						+ giveTo + Economy.ERROR + ".");
				return;
			}

			if (this.economy.countAreas(otherPlayer) >= Economy.MAX_AREAS) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - "
						+ (Economy.online(giveTo) ? Economy.ONLINE
								: Economy.OFFLINE) + giveTo + Economy.ERROR
						+ " can't own any more areas.");
				return;
			}

			for (Area area : this.economy.allAreas()) {
				if (area.areaId != areaId) {
					continue;
				}
				if (!area.owner.equalsIgnoreCase(player.getName())
						&& !player.isAdmin()) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you can't transfer that area.");
					return;
				}

				double transferPrice = Economy.TRANSFER_FEE * area.price();
				String color = Economy.areaColor(area);

				if (this.economy.getBalance(player.getName()) < transferPrice) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - insufficient funds.");
					Economy.sendMessage(player, Economy.ERROR + "It will cost "
							+ Economy.MONEY + Economy.format(transferPrice)
							+ Economy.ERROR + " to transfer that area.");
					return;
				}

				area.owner = giveTo;
				if (area.areaName.isEmpty()) {
					Economy.sendMessage(player, Economy.INFO
							+ "You have transferred area ID "
							+ color
							+ area.areaId
							+ Economy.INFO
							+ " to "
							+ (Economy.online(giveTo) ? Economy.ONLINE
									: Economy.OFFLINE) + giveTo + Economy.INFO
							+ ".");
					if (otherPlayer != null) {
						Economy.sendMessage(otherPlayer, Economy.INFO
								+ Economy.ONLINE
								+ player.getName().toLowerCase() + Economy.INFO
								+ " has given you area ID " + color
								+ area.areaId + Economy.INFO + ".");
					}
				} else {
					Economy.sendMessage(player, Economy.INFO
							+ "You have transferred "
							+ color
							+ area.areaName
							+ Economy.INFO
							+ " to "
							+ (Economy.online(giveTo) ? Economy.ONLINE
									: Economy.OFFLINE) + giveTo + Economy.INFO
							+ ".");
					if (otherPlayer != null) {
						Economy.sendMessage(otherPlayer, Economy.INFO
								+ Economy.ONLINE
								+ player.getName().toLowerCase() + Economy.INFO
								+ " has given you " + color + area.areaName
								+ Economy.INFO + ".");
					}
				}
				this.economy.addToBalance(player.getName().toLowerCase(),
						-transferPrice);
				this.economy.publicFund += transferPrice;
				Economy.sendMessage(player, Economy.INFO + "You spent "
						+ Economy.MONEY + Economy.format(transferPrice)
						+ Economy.INFO + " transferring that area.");
				Economy.sendMessage(player, Economy.INFO
						+ "Your balance has decreased to "
						+ Economy.MONEY
						+ Economy.format(this.economy.getBalance(player
								.getName())) + Economy.INFO + ".");
				return;
			}
		}

		/**
		 * Gets the area for the specified player.
		 * 
		 * @param player
		 *            the player getting the area.
		 * @param split
		 *            parameters.
		 */
		private final void areaGet(Player player, String[] split) {
			// TODO show all areas if inside nested areas
			String playerName;
			if (split.length < 3) {
				playerName = player.getName().toLowerCase();
			} else {
				if (split[2].equals("?")) {
					Economy.sendMessage(player, Economy.COMMAND
							+ "Usage: /area get [playerName]");
					return;
				}
				playerName = split[2].toLowerCase();
			}

			if (!Economy.online(playerName)) {
				Economy.sendMessage(player, Economy.ERROR + "Error - "
						+ Economy.OFFLINE + playerName + Economy.ERROR
						+ " is not online.");
				return;
			}

			Player otherPlayer = etc.getServer().getPlayer(playerName);
			Area area = this.economy.getArea(otherPlayer);
			if (area == null) {
				if (playerName.equalsIgnoreCase(player.getName())) {
					Economy.sendMessage(player, Economy.INFO
							+ "You are not in any areas.");
					if (player.canUseCommand("/area")) {
						Economy.sendMessage(player, Economy.INFO
								+ "Try using the \"/area list\" command.");
					}
				} else {
					Economy.sendMessage(player, Economy.ONLINE + playerName
							+ Economy.INFO + " is not in any areas.");
				}
				return;
			}
			String owner = area.owner;
			String color = Economy.areaColor(area);
			StringBuilder message = new StringBuilder();
			message.append(Economy.INFO);
			if (area.areaName.isEmpty() || area instanceof PersonalArea) {
				if (player.getName().equalsIgnoreCase(playerName)) {
					message.append("You are in ");
					if (player.getName().equalsIgnoreCase(owner)) {
						if (area.areaName.isEmpty()) {
							message.append("your own area. " + color + "("
									+ area.areaId + ")");
						} else {
							message.append(color + area.areaName + Economy.INFO
									+ "." + color + " (" + area.areaId + ")");
						}
					} else {
						message.append((Economy.online(owner) ? Economy.ONLINE
								: Economy.OFFLINE)
								+ owner + "\'s " + Economy.INFO + "area.");
						if (player.isAdmin()) {
							message.append(color + " (" + area.areaId + ")");
						}
					}
				} else {
					message.append((Economy.online(playerName) ? Economy.ONLINE
							: Economy.OFFLINE)
							+ playerName + Economy.INFO + " is in ");
					if (player.getName().equalsIgnoreCase(owner)) {
						if (area.areaName.isEmpty()) {
							message.append("your area. " + color + "("
									+ area.areaId + ")");
						} else {
							message.append(color + area.areaName + Economy.INFO
									+ "." + color + " (" + area.areaId + ")");
						}
					} else {
						message.append((Economy.online(owner) ? Economy.ONLINE
								: Economy.OFFLINE)
								+ owner + "\'s " + Economy.INFO + "area.");
						if (player.isAdmin()) {
							message.append(color + " (" + area.areaId + ")");
						}
					}
				}
			} else {
				if (playerName.equalsIgnoreCase(player.getName())) {
					message.append("You are in ");
				} else {
					message.append((Economy.online(playerName) ? Economy.ONLINE
							: Economy.OFFLINE)
							+ playerName + Economy.INFO + " is in ");
				}
				message.append(color + area.areaName);
				message.append(Economy.INFO + ".");
				if (player.getName().equalsIgnoreCase(owner)
						|| player.isAdmin()) {
					message.append(color);
					message.append(" (");
					message.append(area.areaId);
					message.append(")");
				}
			}
			Economy.sendMessage(player, message.toString());
		}

		/**
		 * Deposits iron/gold/diamond from the players inventory into their
		 * account.
		 * 
		 * @param player
		 *            the player making the deposit.
		 * @param split
		 *            parameters.
		 */
		private final void moneyDeposit(Player player, String[] split) {
			Area area = this.economy.getArea(player);
			if (area == null || !(area instanceof BankArea)) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - you must be in a banking area to make a deposit.");
				if (player.canUseCommand("/area")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Try using the \"/area list\" command.");
				}
				return;
			}

			Inventory inventory = player.getInventory();
			Item[] inventoryArray = inventory.getContents();

			double totalDeposit = 0.0;

			for (int index = 0; index < inventoryArray.length; ++index) {
				if (inventoryArray[index] == null) {
					continue;
				}
				int id = inventoryArray[index].getItemId();
				if (!Economy.ITEM_VALUES.containsKey(id)) {
					continue;
				}
				int pileSize = inventoryArray[index].getAmount();
				double depositAmount = pileSize * Economy.ITEM_VALUES.get(id);
				totalDeposit += depositAmount;
				player.getInventory().removeItem(index);
			}
			if (totalDeposit == 0.0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you don't have anything to deposit.");
				return;
			}
			this.economy.addToBalance(player.getName(), totalDeposit);
			Economy.sendMessage(player, Economy.INFO + "You have deposited "
					+ Economy.MONEY + Economy.format(totalDeposit)
					+ Economy.INFO + ".");
			Economy.sendMessage(player, Economy.INFO
					+ "Your balance has increased to " + Economy.MONEY
					+ Economy.format(this.economy.getBalance(player.getName()))
					+ Economy.INFO + ".");
		}

		/**
		 * Gives the player money. The amount can be positive or negative, but
		 * not 0.
		 * 
		 * @param player
		 *            the player to give money.
		 * @param split
		 *            parameters
		 */
		private final void moneyGive(Player player, String[] split) {
			if (!player.isAdmin()) {
				return;
			}

			if (split.length < 3) {
				Economy.sendMessage(player, Economy.COMMAND
						+ "Usage: /money <give|-g> <amount>");
				return;
			}

			double amount;
			try {
				amount = Double.parseDouble(split[2]);
				amount = Math.round(amount * 100.0) / 100.0;
			} catch (Exception e) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - amount must be a valid number.");
				return;
			}

			if (amount == 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - amount cannot equal 0.");
				return;
			}

			this.economy.addToBalance(player.getName(), amount);
			if (amount > 0) {
				Economy.sendMessage(player, Economy.INFO
						+ "Your balance has increased to "
						+ Economy.MONEY
						+ Economy.format(this.economy.getBalance(player
								.getName())) + Economy.INFO + ".");
			} else {
				Economy.sendMessage(player, Economy.INFO
						+ "Your balance has decreased to "
						+ Economy.MONEY
						+ Economy.format(this.economy.getBalance(player
								.getName())) + Economy.INFO + ".");
			}
		}

		/**
		 * Saves the state of the economy and locations to their respective
		 * files.
		 * 
		 * @param player
		 *            the player saving.
		 * @param split
		 *            parameters (unused)
		 */
		private final void saveEconomy(Player player, String[] split) {
			this.economy.save();
			Economy.sendMessage(player, Economy.INFO + "Economy files saved.");
		}

		@Override
		public void onBlockRightClicked(Player player, Block block, Item item) {
			if (block.getType() == Block.Type.SignPost.getType()
					|| block.getType() == Block.Type.WallSign.getType()) {
				Sign s = (Sign) etc.getServer().getComplexBlock(block);
				if (s.getText(0).trim().equalsIgnoreCase("[sell]")) {
					this.buy(player, s);
				}
				return;
			}
			String currentPlayer = player.getName().toLowerCase();
			ArrayList<Block> blocks;
			String areaType;
			if (this.economy.pendingTradeAreas.containsKey(currentPlayer)) {
				blocks = this.economy.pendingTradeAreas.get(currentPlayer);
				areaType = "trade";
			} else if (this.economy.pendingPersonalAreas
					.containsKey(currentPlayer)) {
				blocks = this.economy.pendingPersonalAreas.get(currentPlayer);
				areaType = "personal";
			} else if (this.economy.pendingBankAreas.containsKey(currentPlayer)) {
				blocks = this.economy.pendingBankAreas.get(currentPlayer);
				areaType = "bank";
			} else {
				return;
			}

			for (Area area : this.economy.allAreas()) {
				if (area.withinArea(block.getX(), block.getY(), block.getZ())) {
					if (area instanceof PersonalArea
							&& area.owner.equalsIgnoreCase(currentPlayer)
							&& (areaType.equals("trade") || areaType
									.equals("bank"))) {
						continue;
					}
					String color = Economy.areaColor(area);
					if (player.getName().equalsIgnoreCase(area.owner)) {
						if (area.areaName.isEmpty()) {
							Economy
									.sendMessage(
											player,
											Economy.ERROR
													+ "Error - that block is already part of area ID "
													+ color + area.areaId
													+ Economy.ERROR + ".");
						} else {
							Economy.sendMessage(player, Economy.ERROR
									+ "Error - that block is already part of "
									+ color + area.areaName + Economy.INFO
									+ "." + color + " (" + area.areaId + ")");
						}
					} else {
						StringBuilder message = new StringBuilder();
						if (area.areaName.isEmpty()
								|| area instanceof PersonalArea) {
							message
									.append(Economy.ERROR
											+ "Error - that block is already part of "
											+ (Economy.online(area.owner) ? Economy.ONLINE
													: Economy.OFFLINE) + "\'s"
											+ Economy.ERROR + " area.");
							if (player.isAdmin()) {
								message
										.append(color + " (" + area.areaId
												+ ")");
							}
						} else {
							message.append(Economy.ERROR
									+ "Error - that block is already part of "
									+ color + area.areaName + Economy.ERROR
									+ ".");
							if (player.isAdmin()) {
								message
										.append(color + " (" + area.areaId
												+ ")");
							}
						}
						Economy.sendMessage(player, message.toString());
					}
					return;
				}
			}

			if (blocks.size() == 3) {
				Block block0 = blocks.get(0);
				Block block1 = blocks.get(1);
				Block block2 = blocks.get(2);
				double xmin = Math.min(block1.getX(), block0.getX());
				double xmax = Math.max(block1.getX(), block0.getX()) + 1;
				double zmin = Math.min(block1.getZ(), block0.getZ());
				double zmax = Math.max(block1.getZ(), block0.getZ()) + 1;
				double ymin = Math.min(block2.getY(), block.getY());
				double ymax = Math.max(block2.getY(), block.getY()) + 1;
				double deltaX = xmax - xmin;
				double deltaY = ymax - ymin;
				double deltaZ = zmax - zmin;
				double floorSize = deltaX * deltaZ;

				if (deltaY <= 1) {
					ymax = ymin + 2;
					deltaY = ymax - ymin;
				}

				if (areaType.equals("trade")
						&& (deltaX > Economy.MAX_TRADE_AREA_LENGTH || deltaZ > Economy.MAX_TRADE_AREA_LENGTH)
						&& !player.isAdmin()) {
					Economy
							.sendMessage(
									player,
									Economy.ERROR
											+ "Error - area too long. Trade area side lengths must be less than "
											+ Economy.MAX_TRADE_AREA_LENGTH);
					this.economy.pendingTradeAreas.remove(currentPlayer);
					return;
				}
				if (areaType.equals("trade")
						&& floorSize > Economy.MAX_TRADE_AREA_SIZE
						&& !player.isAdmin()) {
					Economy
							.sendMessage(
									player,
									Economy.ERROR
											+ "Error - area too large. Maximum trade area size is "
											+ Economy.MAX_TRADE_AREA_SIZE
											+ "blocks^2");
					this.economy.pendingTradeAreas.remove(currentPlayer);
					return;
				}
				if (areaType.equals("bank")
						&& (deltaX > Economy.MAX_BANK_AREA_LENGTH || deltaZ > Economy.MAX_BANK_AREA_LENGTH)
						&& !player.isAdmin()) {
					Economy
							.sendMessage(
									player,
									Economy.ERROR
											+ "Error - area too long. Bank area side lengths must be less than "
											+ Economy.MAX_BANK_AREA_LENGTH
											+ ".");
					this.economy.pendingBankAreas.remove(currentPlayer);
					return;
				}
				if (areaType.equals("bank")
						&& floorSize > Economy.MAX_BANK_AREA_SIZE
						&& !player.isAdmin()) {
					Economy
							.sendMessage(
									player,
									Economy.ERROR
											+ "Error - area too large. Maximum bank area size is "
											+ Economy.MAX_BANK_AREA_SIZE
											+ "blocks^2");
					this.economy.pendingBankAreas.remove(currentPlayer);
					return;
				}

				double areaCost = 0.0;

				if (areaType.equals("trade")) {
					areaCost += floorSize * Economy.TRADE_AREA_COST
							* (1 + Economy.VERTICAL_COST * deltaY);
					if (areaCost > this.economy.getBalance(currentPlayer)) {
						Economy.sendMessage(player, Economy.ERROR
								+ "Error - insufficient funds.");
						Economy.sendMessage(player, Economy.ERROR
								+ "A trading area of that size will cost "
								+ Economy.MONEY + Economy.format(areaCost)
								+ Economy.ERROR + ".");
						this.economy.pendingTradeAreas.remove(currentPlayer);
						Economy.sendMessage(player, Economy.INFO
								+ "You are no longer defining trading areas.");
						return;
					}
					this.economy.addToBalance(currentPlayer, -areaCost);
					this.economy.publicFund += areaCost;
				} else if (areaType.equals("personal")) {
					areaCost += floorSize * Economy.PERSONAL_AREA_COST
							* (1 + Economy.VERTICAL_COST * deltaY);
					if (areaCost > this.economy.getBalance(currentPlayer)) {
						Economy.sendMessage(player, Economy.ERROR
								+ "Error - insufficient funds.");
						Economy.sendMessage(player, Economy.ERROR
								+ "A personal area of that size will cost "
								+ Economy.MONEY + Economy.format(areaCost)
								+ Economy.ERROR + ".");
						this.economy.pendingPersonalAreas.remove(currentPlayer);
						Economy.sendMessage(player, Economy.INFO
								+ "You are no longer defining personl areas.");
						return;
					}
					this.economy.addToBalance(currentPlayer, -areaCost);
					this.economy.publicFund += areaCost;
				} else if (areaType.equals("bank")) {
					areaCost += floorSize * Economy.BANK_AREA_COST
							* (1 + Economy.VERTICAL_COST * deltaY);
					if (areaCost > this.economy.getBalance(currentPlayer)) {
						Economy.sendMessage(player, Economy.ERROR
								+ "Error - insufficient funds.");
						Economy.sendMessage(player, Economy.ERROR
								+ "A banking area of that size will cost "
								+ Economy.MONEY + Economy.format(areaCost)
								+ Economy.ERROR + ".");
						this.economy.pendingBankAreas.remove(currentPlayer);
						Economy.sendMessage(player, Economy.INFO
								+ "You are no longer defining banking areas.");
						return;
					}
					this.economy.addToBalance(currentPlayer, -areaCost);
					this.economy.publicFund += areaCost;
				}
				Area newArea;
				if (areaType.equals("trade")) {
					newArea = new TradeArea(xmin, xmax, ymin, ymax, zmin, zmax,
							currentPlayer);
					this.economy.tradingAreas.add(newArea);
					this.economy.pendingTradeAreas.remove(currentPlayer);
					Economy.sendMessage(player, Economy.INFO
							+ "You have defined a trading area. (Area id "
							+ Economy.TRADE_AREA + newArea.areaId
							+ Economy.INFO + ")");
				} else if (areaType.equals("personal")) {
					newArea = new PersonalArea(xmin, xmax, ymin, ymax, zmin,
							zmax, currentPlayer);
					this.economy.personalAreas.add(newArea);
					this.economy.pendingPersonalAreas.remove(currentPlayer);
					Economy.sendMessage(player, Economy.INFO
							+ "You have defined a personal area. (Area id "
							+ Economy.PERSONAL_AREA_COLOR + newArea.areaId
							+ Economy.INFO + ")");
				} else if (areaType.equals("bank")) {
					newArea = new BankArea(xmin, xmax, ymin, ymax, zmin, zmax,
							currentPlayer);
					this.economy.bankingAreas.add(newArea);
					this.economy.pendingBankAreas.remove(currentPlayer);
					Economy.sendMessage(player, Economy.INFO
							+ "You have defined a banking area. (Area id "
							+ Economy.BANK_AREA_COLOR + newArea.areaId
							+ Economy.INFO + ")");
				}
				Economy.sendMessage(player, Economy.INFO
						+ "You have bought the area for " + Economy.MONEY
						+ Economy.format(areaCost) + Economy.INFO + ".");
				Economy.sendMessage(player, Economy.INFO
						+ "Your balance has decreased to "
						+ Economy.MONEY
						+ Economy
								.format(this.economy.getBalance(currentPlayer))
						+ Economy.INFO + ".");
				if (player.canUseCommand("/area")) {
					Economy
							.sendMessage(
									player,
									Economy.INFO
											+ "You can now name this area with \"/area name\".");
				}
				return;
			}

			blocks.add(block);
			if (blocks.size() == 1) {
				Economy.sendMessage(player, Economy.INFO
						+ "Now right click the other corner.");
			} else if (blocks.size() == 2) {
				Economy.sendMessage(player, Economy.INFO
						+ "Next, right click the floor of your area.");
			} else if (blocks.size() == 3) {
				Economy.sendMessage(player, Economy.INFO
						+ "Finally, right click the ceiling of your area.");
			}
		}

		/**
		 * Removes a trading area. A player can only remove a trading area if
		 * they're the owner of that area or they have admin privelages.
		 * 
		 * @param player
		 *            the player removing the trading area.
		 * @param split
		 *            parameters
		 */
		private final void areaRemove(Player player, String[] split) {
			if (split.length < 3) {
				Economy.sendMessage(player, Economy.COMMAND
						+ "Usage: /area <remove|-r> <areaId|\'all\'>");
				return;
			}

			if (this.economy.tradingAreas.isEmpty()
					&& this.economy.personalAreas.isEmpty()
					&& this.economy.bankingAreas.isEmpty()) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - no areas to remove.");
				return;
			}

			int areaId;
			try {
				areaId = Integer.parseInt(split[2]);
			} catch (Exception e) {
				if (!split[2].equalsIgnoreCase("all")) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - area id must be a valid number.");
					return;
				}
				areaId = -1;
			}
			boolean removed = false;

			Iterator<Area> tradingAreaIterator = this.economy.tradingAreas
					.iterator();
			while (tradingAreaIterator.hasNext()) {
				Area area = tradingAreaIterator.next();
				if (area.areaId == areaId || areaId == -1) {
					if (!player.getName().equalsIgnoreCase(area.owner)) {
						if (areaId == -1) {
							continue;
						}
						if (!player.isAdmin()) {
							Economy.sendMessage(player, Economy.ERROR
									+ "Error - you cannot remove that area.");
							return;
						}
					}
					removed = true;
					tradingAreaIterator.remove();
					if (areaId != -1) {
						Economy.sendMessage(player, Economy.INFO
								+ "Area successfully removed.");
						return;
					}
				}
			}
			Iterator<Area> personalAreaIterator = this.economy.personalAreas
					.iterator();
			while (personalAreaIterator.hasNext()) {
				Area area = personalAreaIterator.next();
				if (area.areaId == areaId || areaId == -1) {
					if (!player.getName().equalsIgnoreCase(area.owner)) {
						if (areaId == -1) {
							continue;
						}
						if (!player.isAdmin()) {
							Economy.sendMessage(player, Economy.ERROR
									+ "Error - you cannot remove that area.");
							return;
						}
					}
					removed = true;
					personalAreaIterator.remove();
					if (areaId != -1) {
						Economy.sendMessage(player, Economy.INFO
								+ "Area successfully removed.");
						return;
					}
				}
			}
			Iterator<Area> bankAreaIterator = this.economy.bankingAreas
					.iterator();
			while (bankAreaIterator.hasNext()) {
				Area area = bankAreaIterator.next();
				if (area.areaId == areaId || areaId == -1) {
					if (!player.getName().equalsIgnoreCase(area.owner)) {
						if (areaId == -1) {
							continue;
						}
						if (!player.isAdmin()) {
							Economy.sendMessage(player, Economy.ERROR
									+ "Error - you cannot remove that area.");
							return;
						}
					}
					removed = true;
					bankAreaIterator.remove();
					if (areaId != -1) {
						Economy.sendMessage(player, Economy.INFO
								+ "Area successfully removed.");
						return;
					}
				}
			}
			if (areaId == -1) {
				if (removed) {
					Economy.sendMessage(player, Economy.INFO
							+ "All areas removed.");
				} else {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you can't remove any areas.");
				}
				return;
			}
			Economy.sendMessage(player, Economy.ERROR
					+ "Error - could not find area id " + areaId + ".");
		}

		/**
		 * Names a trading area. A player can only name an area if they are the
		 * owner or they have admin privelages.
		 * 
		 * @param player
		 *            the player naming the area.
		 * @param split
		 *            parameters.
		 */
		private final void areaName(Player player, String[] split) {
			if (split.length < 4) {
				Economy.sendMessage(player, Economy.COMMAND
						+ "Usage: /area <name|-n> <areaId> <newName>");
				return;
			}

			int areaId;
			try {
				areaId = Integer.parseInt(split[2]);
			} catch (Exception e) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - area id must be a valid number.");
				return;
			}

			StringBuilder newName = new StringBuilder();
			for (int index = 3; index < split.length; ++index) {
				newName.append(split[index]);
				if (index < split.length - 1) {
					newName.append(' ');
				}
			}

			for (Area area : this.economy.allAreas()) {
				if (area.areaId == areaId) {
					if (!(player.getName().equalsIgnoreCase(area.owner) || player
							.isAdmin())) {
						Economy.sendMessage(player, Economy.ERROR
								+ "Error - you cannot rename that area.");
						return;
					}
					double renameFee = area.floorSize() * Economy.RENAME_FEE;
					String color;
					if (area instanceof TradeArea) {
						color = Economy.TRADE_AREA;
						renameFee *= Economy.TRADE_AREA_COST;
					} else if (area instanceof PersonalArea) {
						color = Economy.PERSONAL_AREA_COLOR;
						renameFee *= Economy.PERSONAL_AREA_COST;
					} else if (area instanceof BankArea) {
						color = Economy.BANK_AREA_COLOR;
						renameFee *= Economy.BANK_AREA_COST;
					} else {
						color = Colors.Black;
					}
					if (!area.areaName.isEmpty()
							&& this.economy.getBalance(player.getName()) < renameFee) {
						Economy.sendMessage(player, Economy.ERROR
								+ "Error - insufficient funds.");
						Economy.sendMessage(player, Economy.ERROR
								+ "It will cost " + Economy.MONEY
								+ Economy.format(renameFee) + Economy.ERROR
								+ " to rename that area.");
						Economy.sendMessage(player, Economy.INFO
								+ "Your current balance is "
								+ Economy.format(this.economy.getBalance(player
										.getName())) + Economy.INFO + ".");
						return;
					}
					Economy.sendMessage(player, Economy.INFO
							+ "Area name successfully changed to " + color
							+ newName.toString() + Economy.INFO + ".");
					if (!area.areaName.isEmpty()) {
						this.economy.addToBalance(player.getName(), -renameFee);
						this.economy.publicFund += renameFee;
						Economy.sendMessage(player, Economy.INFO
								+ "Your balance has decreased to "
								+ Economy.MONEY
								+ Economy.format(this.economy.getBalance(player
										.getName())) + Economy.INFO + ".");
					} else if (renameFee > 0) {
						Economy.sendMessage(player, Economy.INFO
								+ "You will be charged " + Economy.MONEY
								+ Economy.format(renameFee) + Economy.INFO
								+ " the next time you rename this area.");
					}
					area.areaName = newName.toString();
					return;
				}
			}

			Economy.sendMessage(player, Economy.ERROR
					+ "Error - could not find area id " + areaId + ".");
		}

		/**
		 * Lists all trading areas. Optionally, the player can specify key:value
		 * pairs for owner, area type, and page number
		 * 
		 * @param player
		 *            the player listing areas.
		 * @param split
		 *            parameters.
		 */
		private final void areaList(final Player player, final String[] split) {
			// TODO add sorting
			final HashMap<String, String> kv = Economy.toKV(split);
			if (split.length > 2) {
				if (split[2].equals("?")) {
					Economy
							.sendMessage(
									player,
									Economy.COMMAND
											+ "Usage: /area <list|-l> [owner:playerName] [type:areaType] [page:pageNumber] [sort:\'distance|name|owner|price\'] [order:\'asc|desc\']");
					return;
				}
			}

			final Condition<Area> displayCondition = new Condition<Area>() {
				@Override
				boolean isValid(Area area) {
					boolean result = true;
					String areaType = null;
					if (kv.containsKey("type")) {
						areaType = kv.get("type").toLowerCase();
					} else if (split.length > 3) {
						areaType = split[3];
					}
					if (areaType != null) {
						if (areaType.equals("bank")) {
							result &= area instanceof BankArea;
						} else if (areaType.equals("trade")) {
							result &= area instanceof TradeArea;
						} else if (areaType.equals("personal")) {
							result &= area instanceof PersonalArea;
						} else {
							Economy.sendMessage(player, Economy.ERROR
									+ "Warning - could not find area type "
									+ areaType + ".");
						}
					}
					String owner = null;
					if (kv.containsKey("owner")) {
						owner = kv.get("owner");
					} else if (split.length > 2) {

						owner = split[2];
					}
					if (owner != null) {
						result &= area.owner.equalsIgnoreCase(owner);
					}
					if (area instanceof PersonalArea
							&& !player.getName().equalsIgnoreCase(area.owner)) {
						result &= player.isAdmin();
					}
					return result;
				}
			};

			if (this.economy.tradingAreas.isEmpty()
					&& this.economy.personalAreas.isEmpty()
					&& this.economy.bankingAreas.isEmpty()) {
				Economy.sendMessage(player, Economy.INFO + "No areas defined.");
				return;
			}

			List<Area> validAreas = new ArrayList<Area>();
			for (Area area : this.economy.allAreas()) {
				if (displayCondition.isValid(area)) {
					validAreas.add(area);
				}
			}

			if (validAreas.size() == 0) {
				Economy.sendMessage(player, Economy.INFO
						+ "No areas to display.");
				return;
			}

			Comparator<Area> c = null;
			String sortType = null;
			if (kv.containsKey("sort")) {
				sortType = kv.get("sort");
			} else if (split.length > 5) {
				sortType = split[5];
			}
			if (sortType != null) {
				String order = "asc";
				if (kv.containsKey("order")) {
					order = kv.get("order");
				} else if (split.length > 6) {
					order = split[6];
				}
				if (!(order.equals("asc") || order.equals("desc"))) {
					Economy
							.sendMessage(
									player,
									Economy.ERROR
											+ "Warning - can only sort ascending or descending.");
					order = "asc";
				}
				final int orderBy = order.equals("asc") ? 1 : -1;

				if (sortType.equals("name")) {
					c = new Comparator<Area>() {
						@Override
						public int compare(Area area1, Area area2) {
							return orderBy
									* area1.areaName.compareTo(area2.areaName);
						}
					};
				} else if (sortType.equals("price")) {
					c = new Comparator<Area>() {
						@Override
						public int compare(Area area1, Area area2) {
							if (area1.price() > area2.price()) {
								return orderBy * 1;
							} else if (area1.price() == area2.price()) {
								return 0;
							} else {
								return orderBy * -1;
							}
						}
					};
				} else if (sortType.equals("owner")) {
					c = new Comparator<Area>() {
						@Override
						public int compare(Area area1, Area area2) {
							return orderBy * area1.owner.compareTo(area2.owner);
						}
					};
				} else if (sortType.equals("distance")) {
					c = new Comparator<Area>() {
						@Override
						public int compare(Area area1, Area area2) {
							double d1 = area1.distanceTo(player);
							double d2 = area2.distanceTo(player);
							if (d1 > d2) {
								return orderBy * 1;
							} else if (d1 == d2) {
								return 0;
							} else {
								return orderBy * -1;
							}
						}
					};
				} else {
					Economy.sendMessage(player, Economy.ERROR
							+ "Warning - could not sort on " + sortType + ".");
				}
			}

			if (c != null) {
				Area[] array = validAreas.toArray(new Area[validAreas.size()]);
				Arrays.sort(array, c);
				validAreas = Arrays.asList(array);
			}

			int page = 1;
			try {
				if (kv.containsKey("page")) {
					page = Integer.parseInt(kv.get("page"));
				} else if (split.length > 4) {
					page = Integer.parseInt(split[4]);
				}
			} catch (Exception e) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - page must be a valid number.");
				return;
			}

			List<Area> output = Economy.getPage(player, validAreas, page);

			for (Area area : output) {
				StringBuilder message = new StringBuilder();
				String areaColor = Economy.areaColor(area);
				message.append(areaColor);
				message.append(area.areaName);
				message.append(" [");
				message.append(Economy.format(area.distanceTo(player)));
				message.append(' ');
				message.append(area.directionFrom(player));
				message.append("]");
				if (area.owner.equalsIgnoreCase(player.getName())
						|| player.isAdmin()) {
					message.append(" (");
					message.append(area.areaId);
					message.append(", ");
					message.append(Economy.MONEY);
					message.append(Economy.format(area.price()));
					message.append(areaColor);
					message.append(")");
				}
				Economy.sendMessage(player, message.toString());
			}
		}

		/**
		 * Lets a player define an area.
		 * 
		 * @param player
		 *            the player defining the area.
		 * @param split
		 *            parameters.
		 */
		private final void areaAdd(Player player, String[] split) {
			String name = player.getName().toLowerCase();
			if (split.length < 3) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /area <add|-a> [\'trade\'|\'personal\'|\'bank\']");
				return;
			}

			if (this.economy.countAreas(player) >= Economy.MAX_AREAS) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you cannot own more than "
						+ Economy.MAX_AREAS + " areas.");
				return;
			}

			if (!(split[2].equalsIgnoreCase("trade")
					|| split[2].equalsIgnoreCase("personal") || split[2]
					.equalsIgnoreCase("bank"))) {
				Economy.sendMessage(player, Economy.COMMAND
						+ "Usage: /area add [\'trade\'|\'personal\'|\'bank\']");
				return;
			}

			if (this.economy.pendingTradeAreas.containsKey(name)) {
				this.economy.pendingTradeAreas.remove(name);
				if (split[2].equalsIgnoreCase("trade")) {
					Economy.sendMessage(player, Economy.INFO
							+ "No longer defining trading areas.");
					return;
				} else if (split[2].equalsIgnoreCase("personal")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Now defining personal areas.");
					this.economy.pendingPersonalAreas.put(name,
							new ArrayList<Block>());
				} else if (split[2].equalsIgnoreCase("bank")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Now defining banking areas.");
					this.economy.pendingBankAreas.put(name,
							new ArrayList<Block>());
				}
			} else if (this.economy.pendingPersonalAreas.containsKey(name)) {
				this.economy.pendingPersonalAreas.remove(name);
				if (split[2].equalsIgnoreCase("trade")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Now defining trading areas.");
					this.economy.pendingTradeAreas.put(name,
							new ArrayList<Block>());
				} else if (split[2].equalsIgnoreCase("personal")) {
					Economy.sendMessage(player, Economy.INFO
							+ "No longer defining personal areas.");
					return;
				} else if (split[2].equalsIgnoreCase("bank")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Now defining banking areas.");
					this.economy.pendingBankAreas.put(name,
							new ArrayList<Block>());
				}
			} else if (this.economy.pendingBankAreas.containsKey(name)) {
				this.economy.pendingBankAreas.remove(name);
				if (split[2].equalsIgnoreCase("trade")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Now defining trading areas.");
					this.economy.pendingTradeAreas.put(name,
							new ArrayList<Block>());
				} else if (split[2].equalsIgnoreCase("personal")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Now defining personal areas.");
					this.economy.pendingPersonalAreas.put(name,
							new ArrayList<Block>());
				} else if (split[2].equalsIgnoreCase("bank")) {
					Economy.sendMessage(player, Economy.INFO
							+ "No longer defining banking areas.");
					return;
				}
			} else {
				if (split[2].equalsIgnoreCase("trade")) {
					this.economy.pendingTradeAreas.put(name,
							new ArrayList<Block>());
					Economy.sendMessage(player, Economy.INFO
							+ "Now defining trading areas.");
				} else if (split[2].equalsIgnoreCase("personal")) {
					this.economy.pendingPersonalAreas.put(name,
							new ArrayList<Block>());
					Economy.sendMessage(player, Economy.INFO
							+ "Now defining personal areas.");
				} else if (split[2].equalsIgnoreCase("bank")) {
					this.economy.pendingBankAreas.put(name,
							new ArrayList<Block>());
					Economy.sendMessage(player, Economy.INFO
							+ "Now defining banking areas.");
				}
			}
			Economy
					.sendMessage(
							player,
							Economy.INFO
									+ "Right click one corner of the area with nothing in your hands.");
		}

		/**
		 * Gets the balance for the player.
		 * 
		 * @param player
		 *            the player checking their balance.
		 * @param split
		 *            parameters (unused).
		 */
		private void moneyBalance(Player player, String[] split) {
			if (split.length > 2 && player.isAdmin()) {
				String other = split[2].toLowerCase();
				if (other.equals("public")) {
					Economy.sendMessage(player, Economy.INFO
							+ "The public fund is at " + Economy.MONEY
							+ Economy.format(this.economy.publicFund)
							+ Economy.INFO + ".");
				} else if (!this.economy.balances.containsKey(other)) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - could not find " + other + ".");
				} else {
					Economy.sendMessage(player,
							(Economy.online(other) ? Economy.ONLINE
									: Economy.OFFLINE)
									+ other
									+ "\'s"
									+ Economy.INFO
									+ " current balance is "
									+ Economy.MONEY
									+ Economy.format(this.economy
											.getBalance(other))
									+ Economy.INFO
									+ ".");
				}
				return;
			}

			Economy.sendMessage(player, Economy.INFO
					+ "Your current balance is " + Economy.MONEY
					+ Economy.format(this.economy.getBalance(player.getName()))
					+ Economy.INFO + ".");
		}

		/**
		 * Sends money from the player to the specified player. The amount must
		 * be positive and the person they send money to must either be online
		 * or have a balance currently.
		 * 
		 * @param player
		 *            the player sending money.
		 * @param split
		 *            parameters.
		 */
		private void moneyPay(Player player, String[] split) {
			if (split.length < 4) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /money <pay|-p> <to:playerName> <amount:paymentAmount>");
				return;
			}

			HashMap<String, String> kv = Economy.toKV(split);

			String payTo = null;
			if (kv.containsKey("to")) {
				payTo = kv.get("to");
			} else if (split.length > 2) {
				payTo = split[2];
			}

			if (payTo == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you must specify a recipient.");
				return;
			}

			if (!(Economy.online(payTo) || this.economy.balances
					.containsKey(payTo))) {
				Economy.sendMessage(player, Economy.INFO + "Could not find "
						+ payTo + ".");
				return;
			}

			double paymentAmount;
			try {
				if (kv.containsKey("amount")) {
					paymentAmount = Double.parseDouble(kv.get("amount"));
				} else if (split.length > 3) {
					paymentAmount = Double.parseDouble(split[3]);
				} else {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you must specify an amount.");
					return;
				}
				paymentAmount = Math.round(paymentAmount * 100.0) / 100.0;
			} catch (Exception e) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - must specify a valid number.");
				return;
			}

			if (paymentAmount <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - payment amount must positive.");
				return;
			}

			double balance = this.economy.getBalance(player.getName());
			if (paymentAmount > balance) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - insufficient funds.");
				Economy.sendMessage(player, Economy.INFO
						+ "Your current balance is " + Economy.MONEY
						+ Economy.format(balance) + Economy.INFO + ".");
				return;
			}

			this.economy.addToBalance(player.getName(), -paymentAmount);
			this.economy.addToBalance(payTo, paymentAmount);

			Economy.sendMessage(player, Economy.INFO + "Payment sent.");
			Player otherPlayer = etc.getServer().getPlayer(payTo);
			if (otherPlayer != null) {
				Economy.sendMessage(otherPlayer, Economy.ONLINE
						+ player.getName() + Economy.INFO + " has sent you "
						+ Economy.MONEY + Economy.format(paymentAmount)
						+ Economy.INFO + ".");
				Economy.sendMessage(otherPlayer, Economy.INFO
						+ "Your balance has increased to " + Economy.MONEY
						+ Economy.format(this.economy.getBalance(payTo))
						+ Economy.INFO + ".");
			}
		}

		/**
		 * Adds an offer using a sign.
		 * 
		 * @param player
		 *            the player adding the offer.
		 * @param sign
		 *            the sign with the offer commands on it.
		 * @return true if the offer was successfully placed.
		 */
		private boolean offerAdd(Player player, Sign sign) {
			if (!player.canUseCommand("/offer")) {
				return false;
			}

			if (this.economy.getOffers(player.getName()).size() >= Economy.MAX_OFFERS) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you can't have more than " + Economy.INFO
						+ Economy.MAX_OFFERS + Economy.ERROR
						+ " offers at once.");
				return false;
			}

			String itemName = sign.getText(2).trim();
			int itemId = Economy.itemId(itemName);
			if (itemId == -1) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - could not find item id for " + itemName
						+ ".");
				return false;
			}

			String amountString = sign.getText(1).trim();
			int amount;
			try {
				amount = Integer.parseInt(amountString);
			} catch (Exception e) {
				if (!amountString.equalsIgnoreCase("all")) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - amount must be a valid number.");
					return false;
				}
				amount = Economy.countItem(player, itemId);
				sign.setText(1, String.valueOf(amount));
				sign.update();
			}

			if (amount <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - amount must be positive.");
				return false;
			}

			int amountInInventory = Economy.countItem(player, itemId);
			if (amount > amountInInventory) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you only have " + amountInInventory + " "
						+ itemName + ".");
				return false;
			}

			double unitPrice;
			try {
				unitPrice = Double.parseDouble(sign.getText(3));
				unitPrice = Math.round(unitPrice * 100.0) / 100.0;
			} catch (Exception e) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - unit price must be a valid number.");
				return false;
			}

			if (unitPrice <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - unit price must be positive.");
				return false;
			}

			player.getInventory().removeItem(itemId, amount);
			player.getInventory().update();

			Offer offer = new Offer(player.getName().toLowerCase(), itemId,
					amount, unitPrice);
			offer.setSign(sign);
			this.economy.addOffer(player.getName().toLowerCase(), offer);

			Economy.sendMessage(player, Economy.INFO
					+ "Your offer has been placed. Your offer id is "
					+ offer.offerId + ".");
			HashSet<String> listeners = this.economy.offerListeners.get(itemId);
			if (listeners == null) {
				return true;
			}
			for (String playerName : listeners) {
				if (playerName.equalsIgnoreCase(player.getName())) {
					continue;
				}
				Player listener = etc.getServer().getPlayer(playerName);
				if (listener == null) {
					continue;
				}
				Economy.sendMessage(listener, Economy.INFO + player.getName()
						+ " has put up an offer for " + amount + " "
						+ Economy.itemName(itemId) + ".");
			}
			return true;
		}

		/**
		 * Puts items up for offer. The player must be in a trading area. They
		 * must specifiy a valid name for an item, a positive amount to sell,
		 * and a positive cost per unit. If they currently have an offer for the
		 * same item and unit price, the offers will be merged.
		 * 
		 * @param player
		 *            the player offering items.
		 * @param split
		 *            parameters.
		 */
		private void offerAdd(Player player, String[] split) {
			if (split.length < 5) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /offer <add|-a> <item:itemName> <amount:itemAmount|'all'> <price:unitPrice>");
				return;
			}

			if (!this.economy.canTrade(player)) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - cannot trade here. Please locate a trading area.");
				if (player.canUseCommand("/area")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Try using the \"/area list\" command.");
				}
				return;
			}

			if (this.economy.getOffers(player.getName()).size() >= Economy.MAX_OFFERS) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you can't have more than " + Economy.INFO
						+ Economy.MAX_OFFERS + Economy.ERROR
						+ " offers at once.");
				return;
			}

			HashMap<String, String> kv = Economy.toKV(split);

			String itemName = null;
			if (kv.containsKey("item")) {
				itemName = kv.get("item");
			} else if (split.length > 2) {
				itemName = split[2];
			}

			if (itemName == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you must specify an item name.");
				return;
			}

			int itemId = Economy.itemId(itemName);
			if (itemId == -1) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - could not find id for " + itemName + ".");
				return;
			}

			String amountString = null;
			if (kv.containsKey("amount")) {
				amountString = kv.get("amount");
			} else if (split.length > 3) {
				amountString = split[3];
			}

			if (amountString == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you must specify an amount.");
				return;
			}

			boolean offerAll = false;
			int amount = 0;
			try {
				amount = Integer.parseInt(amountString);
			} catch (Exception e) {
				if (!amountString.equalsIgnoreCase("all")) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - amount must be a valid number.");
					return;
				}
				offerAll = true;
			}
			int totalAmount = Economy.countItem(player, itemId);

			if (offerAll) {
				amount = totalAmount;
			}

			if (amount <= 0) {
				if (offerAll) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you do not have any " + itemName + ".");
					return;
				}
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - amount must be positive.");
				return;
			}

			if (totalAmount < amount) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you do not have enough " + itemName + ".");
				return;
			}

			double unitPrice;
			try {
				if (kv.containsKey("price")) {
					unitPrice = Double.parseDouble(kv.get("price"));
				} else if (split.length > 4) {
					unitPrice = Double.parseDouble(split[4]);
				} else {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you must specify a unit price.");
					return;
				}
				unitPrice = Math.round(unitPrice * 100.0) / 100.0;
			} catch (Exception e) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - unit price must be a valid number.");
				return;
			}

			if (unitPrice <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - unit price must be positive.");
				return;
			}

			player.getInventory().removeItem(itemId, amount);
			player.getInventory().update();

			ArrayList<Offer> currentOffers = this.economy.sellingOffers
					.get(player.getName().toLowerCase());
			if (currentOffers != null) {
				for (Offer offer : currentOffers) {
					if (offer.itemId == itemId && offer.unitPrice == unitPrice) {
						offer.amount += amount;
						Economy.sendMessage(player, Economy.INFO
								+ "Your offer has been combined with offer id "
								+ offer.offerId + ".");
						return;
					}
				}
			}

			Offer offer = new Offer(player.getName().toLowerCase(), itemId,
					amount, unitPrice);
			this.economy.addOffer(player.getName().toLowerCase(), offer);

			Economy.sendMessage(player, Economy.INFO
					+ "Your offer has been placed. Your offer id is "
					+ offer.offerId + ".");
			HashSet<String> listeners = this.economy.offerListeners.get(itemId);
			if (listeners == null) {
				return;
			}
			for (String playerName : listeners) {
				if (playerName.equalsIgnoreCase(player.getName())) {
					continue;
				}
				Player listener = etc.getServer().getPlayer(playerName);
				if (listener == null) {
					continue;
				}
				Economy.sendMessage(listener, Economy.INFO + player.getName()
						+ " has put up an offer for " + amount + " "
						+ Economy.itemName(itemId) + ".");
			}
			return;
		}

		private void buy(Player player, Sign sign) {
			if (!player.canUseCommand("/buy")) {
				return;
			}

			Offer offer = null;
			for (Offer currentOffer : this.economy.allOffers) {
				Sign s = currentOffer.sellingSign();
				if (s != null && s.hashCode() == sign.hashCode()) {
					offer = currentOffer;
				}
			}

			if (offer == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - couldn't find the proper offer.");
				return;
			}

			if (offer.owner.equalsIgnoreCase(player.getName())) {
				return;
			}

			int buyingAmount = (int) Math.min(offer.amount, this.economy
					.getBalance(player)
					/ offer.unitPrice / (1.0 + Economy.SALES_TAX));

			if (buyingAmount <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - insufficient funds.");
				Economy.sendMessage(player, Economy.INFO
						+ "Your current balance is "
						+ Economy.format(this.economy.getBalance(player)));
				return;
			}
			double price = buyingAmount * offer.unitPrice;
			player.giveItem(offer.itemId, buyingAmount);
			player.getInventory().update();
			this.economy.addToBalance(player, -price * (1.0 + SALES_TAX));
			this.economy.addToBalance(offer.owner, price);
			this.economy.publicFund += price * SALES_TAX;
			Player owner = etc.getServer().getPlayer(offer.owner);
			if (owner != null) {
				Economy.sendMessage(owner, Economy.INFO + player.getName()
						+ " has bought " + buyingAmount + " "
						+ Economy.itemName(offer.itemId) + " for "
						+ Economy.MONEY + Economy.format(price) + Economy.INFO
						+ ".");
				Economy.sendMessage(owner, Economy.INFO
						+ "Your balance has increased to " + Economy.MONEY
						+ Economy.format(this.economy.getBalance(owner))
						+ Economy.INFO + ".");
			}
			Economy.sendMessage(player, Economy.INFO + "You have bought "
					+ buyingAmount + " " + Economy.itemName(offer.itemId)
					+ " for " + Economy.MONEY
					+ Economy.format(price * (1.0 + SALES_TAX)) + Economy.INFO
					+ ".");
			Economy.sendMessage(player, Economy.INFO
					+ "Your balance has decreased to " + Economy.MONEY
					+ Economy.format(this.economy.getBalance(player))
					+ Economy.INFO + ".");
			offer.amount -= buyingAmount;
			this.notifyOfRemove(offer, offer.amount);
			if (offer.amount <= 0) {
				this.economy.removeOffer(offer);
				etc.getServer().setBlockAt(0, sign.getX(), sign.getY(),
						sign.getZ());
				if (owner != null) {
					owner.giveItem(Item.Type.Sign.getId(), 1);
					owner.getInventory().update();
				}
			} else {
				sign.setText(1, String.valueOf(offer.amount));
				sign.update();
			}
		}

		/**
		 * Attempts to buy items. The player must specify a valid item name, a
		 * positive amount, a positive unit price, and must have enough money to
		 * buy as many as (unit price * buying amount). It will buy from the
		 * best offers until either the player buys all their items, runs out of
		 * money, or there are no more offers for that item. Attempts to buy
		 * will only be made on offers whose unit price is less than or equal to
		 * the unit price specified.
		 * 
		 * @param player
		 *            the player buying items.
		 * @param split
		 *            parameters.
		 */
		private void buy(Player player, String[] split) {
			/*
			 * Lets player buy up to "amount" of "id" while the total cost of
			 * transactions is less than "totalPrice"
			 */
			if (split.length < 4) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /buy <item:itemName> <amount:itemAmount> <price:unitPrice>");
				return;
			}

			if (!this.economy.canTrade(player)) {
				Economy
						.sendMessage(
								player,
								Economy.ERROR
										+ "Error - cannot trade here. Please locate a trading area.");
				if (player.canUseCommand("/area")) {
					Economy.sendMessage(player, Economy.INFO
							+ "Try using the \"/area list\" command.");
				}
				return;
			}

			HashMap<String, String> kv = Economy.toKV(split);

			String itemName = null;
			if (kv.containsKey("item")) {
				itemName = kv.get("item");
			} else if (split.length > 1) {
				itemName = split[1];
			}

			if (itemName == null) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - you must specify an item name.");
				return;
			}

			int id = Economy.itemId(itemName);
			if (id == -1) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - could not find id for " + itemName + ".");
				return;
			}

			int buyingAmount;
			try {
				if (kv.containsKey("amount")) {
					buyingAmount = Integer.parseInt(kv.get("amount"));
				} else if (split.length > 2) {
					buyingAmount = Integer.parseInt(split[2]);
				} else {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you must specify an amount.");
					return;
				}
			} catch (Exception e) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - amount must be a valid number.");
				return;
			}

			if (buyingAmount <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - amount must be a positive number.");
				return;
			}

			Inventory inventory = player.getInventory();
			Item[] array = inventory.getContents();
			int canHold = 0;
			for (int index = 0; index < array.length; ++index) {
				if (array[index] == null) {
					canHold += 64;
					continue;
				}
				if (array[index].getItemId() != id) {
					continue;
				}
				canHold += 64 - array[index].getAmount();
			}

			if (buyingAmount > canHold) {
				if (canHold == 0) {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you can't hold any " + itemName + ".");
				} else {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you can't hold " + buyingAmount + " "
							+ itemName + ".");
					Economy.sendMessage(player, Economy.ERROR
							+ "You can only hold " + canHold + " " + itemName
							+ ".");
				}
				return;
			}

			double unitPrice;
			try {
				if (kv.containsKey("price")) {
					unitPrice = Double.parseDouble(kv.get("price"));
				} else if (split.length > 3) {
					unitPrice = Double.parseDouble(split[3]);
				} else {
					Economy.sendMessage(player, Economy.ERROR
							+ "Error - you must specify a unit price.");
					return;
				}
				unitPrice = Math.round(unitPrice * 100.0) / 100.0;
			} catch (Exception e) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - unit price must be a valid number.");
				return;
			}

			if (unitPrice <= 0) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - unit price must be a positive number.");
				return;
			}

			if (this.economy.getBalance(player.getName()) < unitPrice
					* (1.0 + Economy.SALES_TAX)) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Error - insufficient funds.");
				Economy.sendMessage(player, Economy.INFO
						+ "Your current balance is "
						+ Economy.MONEY
						+ Economy.format(this.economy.getBalance(player
								.getName())) + Economy.INFO + ".");
				return;
			}

			double totalMoney = buyingAmount * unitPrice;

			if (this.economy.getBalance(player.getName()) < totalMoney
					* (1.0 + Economy.SALES_TAX)) {
				Economy.sendMessage(player, Economy.ERROR
						+ "Warning - can't buy " + buyingAmount + " "
						+ itemName + ".");
				buyingAmount = (int) (this.economy.getBalance(player.getName())
						/ unitPrice / (1.0 + Economy.SALES_TAX));
				Economy.sendMessage(player, Economy.ERROR
						+ "You will only buy up to " + buyingAmount + " "
						+ itemName + ".");
			}

			while (buyingAmount > 0) {
				Offer bestOffer = null;
				int mostItems = -1;
				double bestPrice = Double.MAX_VALUE;

				Iterator<Offer> iterator = this.economy.offerIterator();
				while (iterator.hasNext()) {
					Offer offer = iterator.next();
					if (offer.itemId == id
							&& !offer.owner.equalsIgnoreCase(player.getName())
							&& offer.unitPrice <= unitPrice) {
						/*
						 * Determine max number of items player can buy at the
						 * current offer's price. If they can't afford more than
						 * what they're trying to buy, they'll buy as much as
						 * they can.
						 */
						int canBuy = Math.min(
								(int) (totalMoney / offer.unitPrice),
								buyingAmount);
						if (canBuy <= 0) {
							continue;
						}
						double price = canBuy * offer.unitPrice;

						/*
						 * Attempt to maximize the number of items bought, while
						 * keeping the price low.
						 */
						if (canBuy > mostItems) {
							mostItems = canBuy;
							bestOffer = offer;
							bestPrice = price;
						} else if (canBuy == mostItems && price < bestPrice) {
							bestOffer = offer;
							bestPrice = price;
						}
					}
				}

				if (bestOffer == null) {
					Economy.sendMessage(player, Economy.INFO
							+ "Could not find a player selling "
							+ Economy.itemName(id) + ".");
					Economy.sendMessage(player, Economy.INFO + "Could not buy "
							+ buyingAmount + " " + Economy.itemName(id) + ".");
					return;
				}

				int sellingAmount = Math.min(mostItems, bestOffer.amount);
				double sellingPrice = sellingAmount * bestOffer.unitPrice;
				this.economy.addToBalance(player.getName(),
						-(sellingPrice * (1.0 + Economy.SALES_TAX)));
				totalMoney -= sellingPrice;
				this.economy.publicFund += sellingPrice * Economy.SALES_TAX;
				this.economy.addToBalance(bestOffer.owner, sellingPrice);
				player.giveItem(id, sellingAmount);
				player.getInventory().update();
				Economy.sendMessage(player, Economy.INFO
						+ "You bought "
						+ sellingAmount
						+ " "
						+ Economy.itemName(id)
						+ " from "
						+ (Economy.online(bestOffer.owner) ? Economy.ONLINE
								: Economy.OFFLINE)
						+ bestOffer.owner
						+ Economy.INFO
						+ " for "
						+ Economy.MONEY
						+ Economy.format(sellingPrice
								* (1.0 + Economy.SALES_TAX)) + Economy.INFO
						+ ".");
				Player seller = etc.getServer().getPlayer(bestOffer.owner);
				if (seller != null) {
					Economy.sendMessage(seller, Economy.ONLINE
							+ player.getName() + Economy.INFO + " bought "
							+ sellingAmount + " " + Economy.itemName(id)
							+ " for " + Economy.MONEY
							+ Economy.format(sellingPrice) + ".");
					Economy.sendMessage(seller, Economy.INFO
							+ "Your balance has increased to "
							+ Economy.MONEY
							+ Economy.format(this.economy
									.getBalance(bestOffer.owner))
							+ Economy.INFO + ".");
				}
				buyingAmount -= sellingAmount;
				bestOffer.amount -= sellingAmount;

				if (bestOffer.amount <= 0) {
					this.economy.removeOffer(bestOffer);
				}

				HashSet<String> listeners = this.economy.offerListeners
						.get(bestOffer.itemId);
				if (listeners == null) {
					continue;
				}
				for (String playerName : listeners) {
					if (playerName.equalsIgnoreCase(player.getName())
							|| playerName.equalsIgnoreCase(bestOffer.owner)) {
						continue;
					}
					Player listener = etc.getServer().getPlayer(playerName);
					if (listener == null) {
						continue;
					}
					if (bestOffer.amount <= 0) {
						Economy.sendMessage(listener, (Economy
								.online(bestOffer.owner) ? Economy.ONLINE
								: Economy.OFFLINE)
								+ bestOffer.owner
								+ "\'s"
								+ Economy.INFO
								+ " offer for "
								+ sellingAmount
								+ " "
								+ Economy.itemName(bestOffer.itemId)
								+ " has been bought.");
					} else {
						Economy.sendMessage(listener, (Economy
								.online(bestOffer.owner) ? Economy.ONLINE
								: Economy.OFFLINE)
								+ bestOffer.owner
								+ "\'s"
								+ Economy.INFO
								+ " offer for "
								+ (sellingAmount + bestOffer.amount)
								+ " "
								+ Economy.itemName(bestOffer.itemId)
								+ " has "
								+ bestOffer.amount + " remaining.");
					}
				}
			}
			Economy.sendMessage(player, Economy.INFO
					+ "Your balance has decreased to " + Economy.MONEY
					+ Economy.format(this.economy.getBalance(player.getName()))
					+ Economy.INFO + ".");
		}

		/**
		 * Lists all current offers. Optionally, the player can specify a player
		 * name to display only offers by that player.
		 * 
		 * @param player
		 *            the player listing offers.
		 * @param split
		 *            parameters.
		 */
		private void offerList(final Player player, final String[] split) {
			final HashMap<String, String> kv = Economy.toKV(split);
			if (split.length > 2 && split[2].equals("?")) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /offer list [seller:playerName] [type:itemName] [maxPrice:unitPrice] [page:pageNumber]");
				return;
			}

			final Condition<Offer> displayCondition = new Condition<Offer>() {
				@Override
				public boolean isValid(Offer offer) {
					boolean result = true;
					String seller = null;
					if (kv.containsKey("seller")) {
						seller = kv.get("seller");
					} else if (split.length > 2) {
						seller = split[2];
					}

					if (seller != null) {
						result &= offer.owner.equalsIgnoreCase(seller);
					}

					String type = null;
					if (kv.containsKey("type")) {
						type = kv.get("type");
					} else if (split.length > 3) {
						type = split[3];
					}

					if (type != null) {
						int id = Economy.itemId(type);
						if (id == -1) {
							Economy.sendMessage(player, Economy.ERROR
									+ "Warning - could not find item id for "
									+ type + ".");
						} else {
							result &= offer.itemId == id;
						}
					}

					String maxPrice = null;
					if (kv.containsKey("maxPrice")) {
						maxPrice = kv.get("maxPrice");
					} else if (split.length > 4) {
						maxPrice = split[4];
					}

					if (maxPrice != null) {
						try {
							int maxCost = Integer.parseInt(maxPrice);
							result &= offer.unitPrice <= maxCost;
						} catch (Exception e) {
							Economy
									.sendMessage(
											player,
											Economy.ERROR
													+ "Error - max unit price must be a valid number.");
						}
					}
					return result;
				}
			};

			int page = 1;
			try {
				if (kv.containsKey("page")) {
					page = Integer.parseInt(kv.get("page"));
				} else if (split.length > 5) {
					page = Integer.parseInt(split[5]);
				}
			} catch (Exception e) {
				sendMessage(player, ERROR
						+ "Error - page must be a valid number.");
				return;
			}

			ArrayList<ArrayList<Offer>> validOffers = new ArrayList<ArrayList<Offer>>();
			for (ArrayList<Offer> currentPlayerOffers : this.economy.sellingOffers
					.values()) {
				ArrayList<Offer> currentValid = new ArrayList<Offer>();
				for (Offer offer : currentPlayerOffers) {
					if (displayCondition.isValid(offer)) {
						currentValid.add(offer);
					}
				}
				if (currentValid.size() > 0) {
					validOffers.add(currentValid);
				}
			}

			if (validOffers.size() == 0) {
				sendMessage(player, INFO + "No offers to display.");
				return;
			}

			for (ArrayList<Offer> offers : getPage(player, validOffers, page)) {
				String owner = offers.get(0).owner;
				StringBuilder message = new StringBuilder();
				message
						.append(online(owner) ? Economy.ONLINE
								: Economy.OFFLINE);
				message.append(owner);
				Player seller = etc.getServer().getPlayer(owner);
				if (seller != null) {
					Area sellerArea = this.economy.getArea(seller);
					if (sellerArea != null && sellerArea instanceof TradeArea
							&& !sellerArea.areaName.isEmpty()) {
						message.append(Economy.TRADE_AREA);
						message.append(" (");
						message.append(sellerArea.areaName);
						message.append(")");
					}
				}
				message.append(Economy.OFFER);
				message.append(": ");

				Iterator<Offer> offerIterator = offers.iterator();
				while (offerIterator.hasNext()) {
					Offer offer = offerIterator.next();
					message.append(offer.toString());
					if (player.getName().equalsIgnoreCase(offer.owner)
							|| player.isAdmin()) {
						message.append("[");
						message.append(offer.offerId);
						message.append("]");
					}
					if (offerIterator.hasNext()) {
						message.append(" ");
					}
				}
				Economy.sendMessage(player, message.toString());
			}
		}

		/**
		 * Cancels an offer based on its offer id. Only the owner of the offer
		 * can cancel it. Optionally, they may specify to remove all offers and
		 * all their current offers will be cleared. When an offer is cancelled,
		 * the items are given back to the player. If the player can't hold all
		 * the items, the offer will not be fully removed and the remaining
		 * fraction of its amount will be left up for sale.
		 * 
		 * @param player
		 *            the player cancelling the offer.
		 * @param split
		 *            parameters.
		 */
		private void offerRemove(Player player, final String[] split) {
			// TODO different filters for removing
			if (split.length < 3) {
				Economy
						.sendMessage(
								player,
								Economy.COMMAND
										+ "Usage: /offer <remove|-r> [id:offerId|\'all\' | item:itemName]");
				return;
			}

			final HashMap<String, String> kv = Economy.toKV(split);

			Condition<Offer> removeCondition = new Condition<Offer>() {
				@Override
				public boolean isValid(Offer offer) {
					boolean result = true;
					String idString = null;
					if (kv.containsKey("id")) {
						idString = kv.get("id");
					} else if (split.length > 2) {
						idString = split[2];
					}

					String itemName = null;
					if (kv.containsKey("item")) {
						itemName = kv.get("item");
					} else if (split.length > 2) {
						itemName = split[2];
					}

					if (idString == null && itemName == null) {
						return false;
					}

					int id = Economy.itemId(itemName);

					if (id == -1) {
						try {
							result &= offer.offerId == Integer
									.parseInt(idString);
						} catch (Exception e) {
							if (!idString.equals("all")) {
								return false;
							}
						}
					} else if (idString != null) {
						result &= itemName(offer.itemId).equalsIgnoreCase(
								itemName);
					}
					return result;
				}
			};

			int removedOffers = 0;
			Iterator<Offer> offerIterator = this.economy.sellingOffers.get(
					player.getName().toLowerCase()).iterator();
			while (offerIterator.hasNext()) {
				Offer offer = offerIterator.next();
				if (!removeCondition.isValid(offer)) {
					continue;
				}
				Sign s = offer.sellingSign();
				if (s != null) {
					etc.getServer().setBlockAt(0, offer.signX, offer.signY,
							offer.signZ);
					player.giveItem(Item.Type.Sign.getId(), 1);
				}
				player.giveItem(offer.itemId, offer.amount);
				++removedOffers;
				offerIterator.remove();
				this.economy.allOffers.remove(offer);
				this.notifyOfRemove(offer, 0);
			}

			if (removedOffers == 0) {
				Economy.sendMessage(player, Economy.INFO
						+ "Could not remove any offers.");
				return;
			}
			Economy.sendMessage(player, Economy.INFO + removedOffers + " offer"
					+ ((removedOffers > 0) ? "s" : "")
					+ " successfully removed.");
		}

		/**
		 * Notifies all listeners for the offer's item id that the offer now has
		 * a certain amount remaining after the offer's owner used a
		 * /removeOffer
		 * 
		 * @param offer
		 *            the offer being cancelled (fully or partially)
		 * @param remaining
		 *            the amount remaining after removal (0 if fully removed, a
		 *            positive integer if partially removed).
		 */
		private final void notifyOfRemove(Offer offer, int remaining) {
			HashSet<String> listeners = this.economy.offerListeners
					.get(offer.itemId);
			if (listeners == null) {
				return;
			}
			for (String playerName : listeners) {
				if (playerName.equalsIgnoreCase(offer.owner)) {
					continue;
				}
				Player listener = etc.getServer().getPlayer(playerName);
				if (listener == null) {
					continue;
				}
				if (remaining <= 0) {
					Economy.sendMessage(listener,
							(Economy.online(offer.owner) ? Economy.ONLINE
									: Economy.OFFLINE)
									+ offer.owner
									+ "\'s"
									+ Economy.INFO
									+ " offer for "
									+ offer.amount
									+ " "
									+ Economy.itemName(offer.itemId)
									+ " has been removed.");
				} else {
					Economy.sendMessage(listener,
							(Economy.online(offer.owner) ? Economy.ONLINE
									: Economy.OFFLINE)
									+ offer.owner
									+ "\'s"
									+ Economy.INFO
									+ " offer for "
									+ offer.amount
									+ " "
									+ Economy.itemName(offer.itemId)
									+ " has "
									+ remaining + " remaining.");
				}
			}
		}
	}

	/**
	 * The global area counter.
	 */
	private static int areaCount;

	public class Area implements Serializable {

		private static final long serialVersionUID = 2L;

		/**
		 * The corner points for this Area.
		 */
		public final double xmin, xmax, ymin, ymax, zmin, zmax;

		/**
		 * The unique area id for this Area.
		 */
		public final int areaId;

		/**
		 * The owner of this area.
		 */
		private String owner;

		/**
		 * The name of this area. Blank by default.
		 */
		private String areaName;

		/**
		 * Creates an Area with the given parameters.
		 * 
		 * @param minX
		 * @param maxX
		 * @param minZ
		 * @param maxZ
		 * @param owner
		 *            the owner of this Area.
		 */
		public Area(double minX, double maxX, double minY, double maxY,
				double minZ, double maxZ, String owner) {
			this.xmin = minX;
			this.xmax = maxX;
			this.ymin = minY;
			this.ymax = maxY;
			this.zmin = minZ;
			this.zmax = maxZ;
			this.areaId = Economy.areaCount++;
			if (Economy.areaCount < 0) {
				Economy.logger.log(Level.WARNING,
						"Area id count has overflowed.");
			}
			this.owner = owner;
			this.areaName = "";
		}

		/**
		 * Gets the owner of this area.
		 * 
		 * @return the area's owner.
		 */
		public String getOwner() {
			return this.owner;
		}

		/**
		 * Sets a new owner for this area.
		 * 
		 * @param owner
		 *            the new owner.
		 */
		public final void setOwner(String owner) {
			this.owner = owner;
		}

		/**
		 * Gets the name of this area.
		 * 
		 * @return the area's name.
		 */
		public String getName() {
			return this.areaName;
		}

		/**
		 * Sets the name of this area.
		 * 
		 * @param name
		 *            the new name.
		 */
		public final void setName(String name) {
			this.areaName = name;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Area)) {
				return false;
			}
			Area otherArea = (Area) other;
			return this.areaId == otherArea.areaId;
		}

		/**
		 * Determines the distance from a player to the center of this area.
		 * 
		 * @param player
		 *            the player.
		 * @return the area from the player to the center of this area.
		 */
		public double distanceTo(Player player) {
			double centerX = this.xmin + (this.xmax - this.xmin) / 2.0;
			double centerZ = this.zmin + (this.zmax - this.zmin) / 2.0;
			double deltaX = player.getX() - centerX;
			double deltaZ = player.getZ() - centerZ;
			return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
		}

		/**
		 * Gets the compass direction from a player to this area.
		 * 
		 * @param player
		 *            the player.
		 * @return the direction from the player towards this area.
		 */
		public String directionFrom(Player player) {
			double centerX = this.xmin + (this.xmax - this.xmin) / 2.0;
			double centerZ = this.zmin + (this.zmax - this.zmin) / 2.0;
			double deltaX = centerX - player.getX();
			double deltaZ = centerZ - player.getZ();
			double degrees = -(180 * Math.atan2(deltaX, deltaZ) / Math.PI + 90);
			if (degrees < 0) {
				degrees += 360;
			} else if (degrees > 360) {
				degrees -= 360;
			}
			return etc.getCompassPointForDirection(degrees);
		}

		/**
		 * Gets the floor size of this area (corner to corner).
		 * 
		 * @return the floor space.
		 */
		public final double floorSize() {
			return (this.xmax - this.xmin) * (this.zmax - this.zmin);
		}

		/**
		 * Gets the total number of blocks in this area.
		 * 
		 * @return the total number of blocks.
		 */
		public final double totalSize() {
			return this.floorSize() * (this.ymax - this.ymin);
		}

		/**
		 * Gets the price of this area.
		 * 
		 * @return the price.
		 */
		public double price() {
			return this.floorSize()
					* (1 + Economy.VERTICAL_COST * (this.ymax - this.ymin));
		}

		/**
		 * Determines whether the given point is inside this Area.
		 * 
		 * @param x
		 * @param z
		 * @return true if (x, z) is inside this area, false otherwise.
		 */
		public final boolean withinArea(double x, double y, double z) {
			return (x >= this.xmin && x <= this.xmax && y >= this.ymin
					&& y <= this.ymax && z >= this.zmin && z <= this.zmax);
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			if (!this.areaName.isEmpty()) {
				result.append(this.areaName);
				result.append(": ");
			}
			result.append("(");
			result.append((int) this.xmin);
			result.append(", ");
			result.append((int) this.ymin);
			result.append(", ");
			result.append((int) this.zmin);
			result.append(") to (");
			result.append((int) this.xmax);
			result.append(", ");
			result.append((int) this.ymax);
			result.append(", ");
			result.append((int) this.zmax);
			result.append(")");
			return result.toString();
		}
	}

	/**
	 * 
	 * A Bank area is a place where players can deposit and withdraw money and
	 * items.
	 * 
	 * @author lackeybp. Created Dec 22, 2010.
	 */
	public class BankArea extends Area implements Serializable {
		private static final long serialVersionUID = 1231436205851667615L;

		/**
		 * Creates a bank area with the given parameters.
		 * 
		 * @param minX
		 * @param maxX
		 * @param minZ
		 * @param maxZ
		 * @param owner
		 *            the owner of this Area.
		 */
		public BankArea(double minX, double maxX, double minY, double maxY,
				double minZ, double maxZ, String owner) {
			super(minX, maxX, minY, maxY, minZ, maxZ, owner);
		}

		@Override
		public double price() {
			return Economy.BANK_AREA_COST * super.price();
		}
	}

	/**
	 * The BankSlot class is used for storing id:amount pairs for bank accounts.
	 * 
	 * @author lackeybp. Created Dec 21, 2010.
	 */
	public class BankSlot implements Comparable<BankSlot>, Serializable {
		private static final long serialVersionUID = 11L;
		public final int itemId;
		private int amount;

		/**
		 * Creates a bank slot for the specified item.
		 * 
		 * @param itemId
		 *            the item's id.
		 */
		public BankSlot(int itemId) {
			this(itemId, 0);
		}

		/**
		 * Creates a bank slot for the specified item with the specified amount.
		 * 
		 * @param itemId
		 *            the item's id.
		 * @param amount
		 *            the amount of item in the slot.
		 */
		public BankSlot(int itemId, int amount) {
			this.itemId = itemId;
			this.amount = amount;
		}

		/**
		 * Gets the amount in this bank slot.
		 * 
		 * @return the amount.
		 */
		public final int getAmount() {
			return this.amount;
		}

		/**
		 * Sets the amount in this bank slot.
		 * 
		 * @param amount
		 *            the new amount.
		 */
		public final void setAmount(int amount) {
			this.amount = amount;
		}

		@Override
		public int compareTo(BankSlot other) {
			return Economy.itemName(this.itemId).compareTo(
					Economy.itemName(other.itemId));
		}
	}

	/**
	 * The Condition class allows for different types of conditions to be
	 * evaluated for different purposes.
	 * 
	 * @author noobaholic. Created Dec 12, 2010.
	 * @param <T>
	 */
	private static abstract class Condition<T> implements Serializable {
		private static final long serialVersionUID = 3L;

		abstract boolean isValid(T parameter);
	}

	/**
	 * A personal area can only be edited by its owner.
	 * 
	 * @author lackeybp. Created Dec 22, 2010.
	 */
	public class PersonalArea extends Area {
		private static final long serialVersionUID = 1175775527803459405L;

		/**
		 * Creates a personal area with the given parameters.
		 * 
		 * @param minX
		 * @param maxX
		 * @param minZ
		 * @param maxZ
		 * @param owner
		 *            the owner of this Area.
		 */
		public PersonalArea(double minX, double maxX, double minY, double maxY,
				double minZ, double maxZ, String owner) {
			super(minX, maxX, minY, maxY, minZ, maxZ, owner);
		}

		@Override
		public double price() {
			return Economy.PERSONAL_AREA_COST * super.price();
		}
	}

	/**
	 * Trade areas area places where players can buy and sell items along with
	 * hosting auctions.
	 * 
	 * @author lackeybp. Created Dec 22, 2010.
	 */
	public class TradeArea extends Area {
		private static final long serialVersionUID = 7906766924747483195L;

		/**
		 * Creates a trading area with the given parameters.
		 * 
		 * @param minX
		 * @param maxX
		 * @param minZ
		 * @param maxZ
		 * @param owner
		 *            the owner of this Area.
		 */
		public TradeArea(double minX, double maxX, double minY, double maxY,
				double minZ, double maxZ, String owner) {
			super(minX, maxX, minY, maxY, minZ, maxZ, owner);
		}

		@Override
		public double price() {
			return Economy.TRADE_AREA_COST * super.price();
		}
	}
}