Nxt2Wallet
=========

Nxt2Wallet supports sending and receiving ARDR as well as Nxt child coins.  It communicates with the Nxt2 node using an HTTP/HTTPS connection to the API port.  Your secret phrase is never sent to the API server, so it is safe to use a remote node.  The only data that is saved locally is the contact list.  All other account data is maintained by the Nxt2 network.

Each Nxt coin is shown in a separate tabbed pane.  You can right-click on a transaction to get a popup menu.  The Send Money and View Exchange buttons apply to the current tab.  The View Contacts button is the same for all tabs since the contacts list is shared by all of the coins.

Child transactions are bundled together for inclusion in the Nxt block chain.  Each bundler advertised a minimum rate and will not accept transactions with a fee lower than this rate.  Transactions have a fixed ARDR cost.  If a bundler advertises a rate of 0.10, for example, then the child transaction fee must be at least 0.10 for each ARDR.  So, if a transaction costs 40 ARDR, then the child transaction fee must be at least 4.  Nxt2Wallet will pre-fill the rate field with the best bundler rate currently available and you can change it if desired.  Transactions submitted directly to the block chain (FXT transactions) do not use a bundler and the transaction fee must be the required number of ARDR for the transaction.

The View Exchange button shows Coin Exchange offers for the current coin.  You can enter a coin exchange order for the current coin to either match an existing order or to create a new order with a different price.  The exchange amount is the number of coins you want to exchange and the price is how much you want to pay for each coin you receive.  Your exchange order will be filled at this price or at a lower price depending on the matching orders.


Build
=====

I use the Netbeans IDE but any build environment with Maven and the Java compiler available should work.  The documentation is generated from the source code using javadoc.

Here are the steps for a manual build.  You will need to install Maven 3 and Java SE Development Kit 8 if you don't already have them.

  - Create the executable: mvn clean package    
  - [Optional] Copy target/Nxt2Wallet-v.r.m.jar and lib/* to wherever you want to store the executables.   
  - Create a shortcut to start Nxt2Wallet using java.exe for a command window or javaw.exe for GUI only.     


Runtime Options
===============

The following command-line options can be specified using -Dname=value

  - nxt.datadir=directory-path		
    Specifies the application data directory. Application data will be stored in a system-specific directory if this option is omitted:		
	    - Linux: user-home/.Nxt2Wallet	
		- Mac: user-home/Library/Application Support/Nxt2Wallet	
		- Windows: user-home\AppData\Roaming\Nxt2Wallet	
	
  - java.util.logging.config.file=file-path		
    Specifies the logger configuration file. The logger properties will be read from 'logging.properties' in the application data directory. If this file is not found, the 'java.util.logging.config.file' system property will be used to locate the logger configuration file. If this property is not defined, the logger properties will be obtained from jre/lib/logging.properties.
	
    JDK FINE corresponds to the SLF4J DEBUG level	
	JDK INFO corresponds to the SLF4J INFO level	
	JDK WARNING corresponds to the SLF4J WARN level		
	JDK SEVERE corresponds to the SLF4J ERROR level		

The following configuration options can be specified in Nxt2Wallet.conf.  This file is optional and must be in the application directory in order to be used.	

  - connect=host	
    Specifies the Nxt2 node host name and defaults to 'localhost'		
	
  - apiport=port		
	Specifies the API port of the Nxt2 node and defaults to 27876.  Use 26876 for testnet.    
    
  - useSSL=boolean      
    Specify 'true' to use HTTPS or 'false' to use HTTP to connect to the NRS node.  The default is 'true'.  HTTP is always used when connected to 'localhost'.
    
  - account=id      
    Specify the Nxt account as either an identifier or a Reed-Solomon string.  This parameter can be repeated to define multiple accounts.  You will be prompted to enter the account if this parameter is not specified.  The account must exist before you can use Nxt2Wallet.  An account is created by sending coins or a message to the account from an existing account.    
