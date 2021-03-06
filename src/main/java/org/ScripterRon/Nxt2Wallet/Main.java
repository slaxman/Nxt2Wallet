/*
 * Copyright 2016 Ronald W Hoffman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.Nxt2Wallet;

import org.ScripterRon.Nxt2API.Balance;
import org.ScripterRon.Nxt2API.Chain;
import org.ScripterRon.Nxt2API.Crypto;
import org.ScripterRon.Nxt2API.IdentifierException;
import org.ScripterRon.Nxt2API.KeyException;
import org.ScripterRon.Nxt2API.Nxt;
import org.ScripterRon.Nxt2API.NxtException;
import org.ScripterRon.Nxt2API.Response;
import org.ScripterRon.Nxt2API.Transaction;
import org.ScripterRon.Nxt2API.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Nxt2Wallet is a wallet used for sending and receiving Nxt coins
 */
public class Main {

    /** Logger instance */
    public static final Logger log = LoggerFactory.getLogger("org.ScripterRon.Nxt2Wallet");

    /** File separator */
    public static String fileSeparator;

    /** Line separator */
    public static String lineSeparator;

    /** User home */
    public static String userHome;

    /** Operating system */
    public static String osName;

    /** Application identifier */
    public static String applicationID;

    /** Application name */
    public static String applicationName;

    /** Application version */
    public static String applicationVersion;

    /** Application properties */
    public static Properties properties;

    /** Data directory */
    public static String dataPath;

    /** Application properties file */
    private static File propFile;

    /** Contacts file */
    private static File contactsFile;

    /** Main application window */
    public static MainWindow mainWindow;

    /** Nxt node host name */
    public static String connect = "localhost";

    /** API port */
    public static int apiPort = 27876;

    /** Use HTTPS connections */
    public static boolean useSSL = true;

    /** Nxt node application */
    public static String nxtApplication;

    /** Nxt node version */
    public static String nxtVersion;

    /** Secret phrases */
    public static List<String> secretPhrases = new ArrayList<>();

    /** Accounts */
    public static List<Long> accounts = new ArrayList<>();

    /** Account password phrase */
    public static String passPhrase;

    /** Account identifier */
    public static long accountId;

    /** Account Reed-Solomon identifier */
    public static String accountRsId;

    /** Account name */
    public static String accountName;

    /** Current block height */
    public static int blockHeight;

    /** Contacts list */
    public static final List<Contact> contactsList = new LinkedList<>();

    /** Contact lookup by account identifier */
    public static final Map<Long, Contact> contactsMap = new HashMap<>();

    /** Child transaction bundler rates */
    public static final Map<Integer, Long> bundlerRates = new HashMap<>();

    /** Account confirmed transactions */
    public static List<Transaction> accountTransactions = new ArrayList<>();

    /** Account unconfirmed transactions */
    public static List<Transaction> unconfirmedTransactions = new ArrayList<>();

    /** Account balances */
    public static Map<Integer, Balance> accountBalance = new HashMap<>();

    /** Application lock file */
    private static RandomAccessFile lockFile;

    /** Application lock */
    private static FileLock fileLock;

    /** Deferred exception text */
    private static String deferredText;

    /** Deferred exception */
    private static Throwable deferredException;

    /**
     * Handles program initialization
     *
     * @param   args                Command-line arguments
     */
    public static void main(String[] args) {
        try {
            fileSeparator = System.getProperty("file.separator");
            lineSeparator = System.getProperty("line.separator");
            userHome = System.getProperty("user.home");
            osName = System.getProperty("os.name").toLowerCase();
            //
            // Process command-line options
            //
            dataPath = System.getProperty("nxt.datadir");
            if (dataPath == null) {
                if (osName.startsWith("win"))
                    dataPath = userHome+"\\Appdata\\Roaming\\Nxt2Wallet";
                else if (osName.startsWith("linux"))
                    dataPath = userHome+"/.Nxt2Wallet";
                else if (osName.startsWith("mac os"))
                    dataPath = userHome+"/Library/Application Support/Nxt2Wallet";
                else
                    dataPath = userHome+"/Nxt2Wallet";
            }
            //
            // Create the data directory if it doesn't exist
            //
            File dirFile = new File(dataPath);
            if (!dirFile.exists())
                dirFile.mkdirs();
            //
            // Initialize the logging properties from 'logging.properties'
            //
            File logFile = new File(dataPath+fileSeparator+"logging.properties");
            if (logFile.exists()) {
                FileInputStream inStream = new FileInputStream(logFile);
                LogManager.getLogManager().readConfiguration(inStream);
            }
            //
            // Use the brief logging format
            //
            BriefLogFormatter.init();
            //
            // Process configuration file options
            //
            processConfig();
            //
            // Get the application build properties
            //
            Class<?> mainClass = Class.forName("org.ScripterRon.Nxt2Wallet.Main");
            try (InputStream classStream = mainClass.getClassLoader().getResourceAsStream("META-INF/application.properties")) {
                if (classStream == null)
                    throw new IllegalStateException("Application build properties not found");
                Properties applicationProperties = new Properties();
                applicationProperties.load(classStream);
                applicationID = applicationProperties.getProperty("application.id");
                applicationName = applicationProperties.getProperty("application.name");
                applicationVersion = applicationProperties.getProperty("application.version");
            }
            log.info(String.format("%s Version %s", applicationName, applicationVersion));
            log.info(String.format("Application data path: %s", dataPath));
            log.info(String.format("Using Nxt node at %s://%s:%d",
                    (connect.equals("localhost") ? "http" : (useSSL ? "https" : "http")), connect, apiPort));
            //
            // Open the application lock file
            //
            lockFile = new RandomAccessFile(dataPath+fileSeparator+".lock", "rw");
            fileLock = lockFile.getChannel().tryLock();
            if (fileLock == null) {
                JOptionPane.showMessageDialog(null, "Nxt2Wallet is already running", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            //
            // Load the saved application properties
            //
            propFile = new File(dataPath+fileSeparator+"Nxt2Wallet.properties");
            properties = new Properties();
            if (propFile.exists()) {
                try (FileInputStream in = new FileInputStream(propFile)) {
                    properties.load(in);
                }
            }
            //
            // Load the contacts
            //
            contactsFile = new File(dataPath+fileSeparator+"Nxt2Wallet.contacts");
            if (contactsFile.exists()) {
                try {
                    try (FileInputStream inStream = new FileInputStream(contactsFile)) {
                        while (true) {
                            Contact contact = new Contact(inStream);
                            contactsList.add(contact);
                            contactsMap.put(contact.getAccountId(), contact);
                        }
                    }
                } catch (EOFException exc) {
                    log.info(String.format("%d contacts read", contactsList.size()));
                } catch (IOException exc) {
                    logException("Unable to read contacts", exc);
                }
            }
            //
            // Start the wallet
            //
            startup();
        } catch (Exception exc) {
            logException("Exception during program initialization", exc);
        }
    }

    /**
     * Start our services
     */
    private static void startup() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            //
            // Initialize the Nxt API library
            //
            Nxt.init(connect, apiPort, useSSL);
            //
            // Get the account if one wasn't provided
            //
            if (accounts.isEmpty()) {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    while (accounts.isEmpty()) {
                        try {
                            String accountString = JOptionPane.showInputDialog("Enter the account");
                            if (accountString == null || accountString.length() == 0)
                                break;
                            accountString = accountString.toUpperCase().trim();
                            if (accountString.startsWith("NXT-")) {
                                accounts.add(Utils.parseAccountRsId(accountString));
                            } else {
                                accounts.add(Utils.stringToId(accountString));
                            }
                            secretPhrases.add("");
                        } catch (IdentifierException exc) {
                            JOptionPane.showMessageDialog(null, "Invalid account", "Account Error",
                                                          JOptionPane.ERROR_MESSAGE);
                        } catch (Exception exc) {
                            Main.log.error("Exception while getting account", exc);
                            Main.logException("Exception while getting account", exc);
                            break;
                        }
                    }
                });
                if (accounts.isEmpty())
                    System.exit(0);
            }
            passPhrase = secretPhrases.get(0);
            accountId = accounts.get(0);
            accountRsId = Utils.getAccountRsId(accountId);
            //
            // Get the local Nxt node state
            //
            Response response = Nxt.getBlockchainStatus();
            nxtApplication = response.getString("application");
            nxtVersion = response.getString("version");
            blockHeight = response.getInt("numberOfBlocks") - 1;
            log.info(String.format("%s Version %s: Chain height %,d",
                                   nxtApplication, nxtVersion, blockHeight));
            //
            // Get the child transaction bundler rates
            //
            response = Nxt.getBundlerRates();
            List<Response> rates = response.getObjectList("rates");
            rates.forEach(rate -> {
                bundlerRates.put(rate.getInt("chain"), rate.getLong("minRateNQTPerFXT"));
             });
            //
            // Get the initial account information
            //
            accountName = getAccount(accountId, accountTransactions, unconfirmedTransactions, accountBalance);
            //
            // Start the GUI
            //
            javax.swing.SwingUtilities.invokeLater(() -> {
                createAndShowGUI();
            });
        } catch (IOException exc) {
            log.error("Unable to get initial account information", exc);
            logException("Unable to get initial account information", exc);
            shutdown();
        } catch (Exception exc) {
            log.error("Exception while starting account services", exc);
            logException("Exception while starting account services", exc);
            shutdown();
        }
    }

    /**
     * Get account information
     *
     * @param   accountId               Account identifier
     * @param   transactionList         Account transactions list
     * @param   unconfirmedList         Unconfirmed account transactions list
     * @param   balances                Account balances
     * @return                          Account name
     * @throws  IdentifierException     Invalid Nxt object identifier
     * @throws  IOException             Unable to issue Nxt API request
     */
    public static String getAccount(long accountId, List<Transaction> transactionList,
                                List<Transaction> unconfirmedList, Map<Integer, Balance> balances)
                                throws IdentifierException, IOException {
        Response response = Nxt.getAccount(accountId);
        String name = response.getString("name");
        for (Chain chain : Nxt.getAllChains()) {
            List<Response> txList;
            for (int index=0; ; index+=50) {
                txList = Nxt.getBlockchainTransactions(accountId, chain, index, index+49);
                if (!txList.isEmpty())
                    transactionList.addAll(Transaction.processTransactions(txList));
                if (txList.size() < 50)
                    break;
            }
            txList = Nxt.getUnconfirmedTransactions(accountId, chain);
            if (!txList.isEmpty()) {
                unconfirmedList.addAll(Transaction.processTransactions(txList));
            }
            balances.put(chain.getId(), Nxt.getBalance(accountId, chain));
        }
        return name;
    }

    /**
     * Create and show our application GUI
     *
     * This method is invoked on the AWT event thread to avoid timing
     * problems with other window events
     */
    private static void createAndShowGUI() {
        try {
            //
            // Use the normal window decorations as defined by the look-and-feel
            // schema
            //
            JFrame.setDefaultLookAndFeelDecorated(true);
            //
            // Create the main application window
            //
            mainWindow = new MainWindow();
            //
            // Show the application window
            //
            mainWindow.pack();
            mainWindow.setVisible(true);
        } catch (Exception exc) {
            log.error("Unable to create GUI", exc);
            Main.logException("Unable to create GUI", exc);
        }
    }

    /**
     * Shutdown and exit
     */
    public static void shutdown() {
        //
        // Save the application properties
        //
        saveProperties();
        //
        // Close the application lock file
        //
        try {
            fileLock.release();
            lockFile.close();
        } catch (IOException exc) {
        }
        //
        // All done
        //
        System.exit(0);
    }

    /**
     * Save the contacts
     */
    public static void saveContacts() {
        try (FileOutputStream outStream = new FileOutputStream(contactsFile)) {
            for (Contact contact : contactsList)
                contact.getBytes(outStream);
        } catch (IOException exc) {
            logException("Unable to save contacts", exc);
        }
    }

    /**
     * Save the application properties
     */
    public static void saveProperties() {
        try (FileOutputStream out = new FileOutputStream(propFile)) {
            properties.store(out, "Nxt2Wallet Properties");
        } catch (IOException exc) {
            Main.logException("Exception while saving application properties", exc);
        }
    }

    /**
     * Process the configuration file
     *
     * @throws      IllegalArgumentException    Invalid configuration option
     * @throws      IOException                 Unable to read configuration file
     */
    private static void processConfig() throws IOException, IllegalArgumentException {
        //
        // Use the defaults if there is no configuration file
        //
        File configFile = new File(dataPath + Main.fileSeparator + "Nxt2Wallet.conf");
        if (!configFile.exists())
            return;
        //
        // Process the configuration file
        //
        String option;
        String value = null;
        try (BufferedReader in = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line=in.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#')
                    continue;
                int sep = line.indexOf('=');
                if (sep < 1)
                    throw new IllegalArgumentException(String.format("Invalid configuration option: %s", line));
                option = line.substring(0, sep).trim().toLowerCase();
                value = line.substring(sep+1).trim();
                switch (option) {
                    case "apiport":
                        apiPort = Integer.valueOf(value);
                        break;
                    case "connect":
                        connect = value.toLowerCase();
                        break;
                    case "account":
                        value = value.toUpperCase();
                        if (value.startsWith("NXT-")) {
                                accounts.add(Utils.parseAccountRsId(value));
                            } else {
                                accounts.add(Utils.stringToId(value));
                            }
                            secretPhrases.add("");
                        break;
                    case "passphrase":
                        secretPhrases.add(value);
                        accounts.add(Utils.getAccountId(Crypto.getPublicKey(value)));
                        break;
                    case "usessl":
                        useSSL = Boolean.valueOf(value);
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("Invalid configuration option: %s", line));
                }
            }
        } catch (IdentifierException exc) {
            logException("Account identifier '" + value + "' is not valid", exc);
        } catch (NumberFormatException exc) {
            logException("'" + value + "' is not a valid number", exc);
        } catch (KeyException exc) {
            logException("Unable to generate account identifier from secret phrase", exc);
        } catch (IOException exc) {
            logException("Unable to read configuration file", exc);
        }
    }

    /**
     * Display a dialog when an exception occurs.
     *
     * @param       text        Text message describing the cause of the exception
     * @param       exc         The Java exception object
     */
    public static void logException(String text, Throwable exc) {
        if (SwingUtilities.isEventDispatchThread()) {
            StringBuilder string = new StringBuilder(512);
            //
            // Display our error message
            //
            string.append("<html><b>");
            string.append(text);
            string.append("</b><br>");
            //
            // Display the exception object
            //
            string.append("<b>");
            if (exc instanceof NxtException) {
                string.append(exc.getMessage());
            } else {
                string.append(exc.toString());
            }
            string.append("</b><br><br>");
            //
            // Display the stack trace
            //
            if (!(exc instanceof NxtException)) {
                StackTraceElement[] trace = exc.getStackTrace();
                int count = 0;
                for (StackTraceElement elem : trace) {
                    string.append(elem.toString());
                    string.append("<br>");
                    if (++count == 25)
                        break;
                }
            }
            string.append("</html>");
            JOptionPane.showMessageDialog(mainWindow, string, "Error", JOptionPane.ERROR_MESSAGE);
        } else if (deferredException == null) {
            deferredText = text;
            deferredException = exc;
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    Main.logException(deferredText, deferredException);
                    deferredException = null;
                    deferredText = null;
                });
            } catch (Exception logexc) {
                log.error("Unable to log exception during program initialization");
            }
        }
    }
}
