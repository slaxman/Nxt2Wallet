Nxt2Wallet
=========

Nxt2Wallet supports sending and receiving ARDR as well as Nxt child coins.  It communicates with the Nxt2 node using an HTTP/HTTPS connection to the API port.  Your secret phrase is never sent to the API server, so it is safe to use a remote node.  The only data that is saved locally is the contact list.  All other account data is maintained by the Nxt2 network.


Build
=====

I use the Netbeans IDE but any build environment with Maven and the Java compiler available should work.  The documentation is generated from the source code using javadoc.

Here are the steps for a manual build.  You will need to install Maven 3 and Java SE Development Kit 8 if you don't already have them.

  - Create the executable: mvn clean package    
  - [Optional] Create the documentation: mvn javadoc:javadoc    
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
	Specifies the API port of the Nxt2 node and defaults to 27876.
    
  - useSSL=boolean      
    Specify 'true' to use HTTPS or 'false' to use HTTP to connect to the NRS node.  The default is 'true'.  HTTP is always used when connected to 'localhost'.
    
  - allowNameMismatch=boolean       
    Specify 'true' to allow an HTTPS connection or 'false' to reject an HTTPS connection if the host name does not match the SSL certificate name.  The default is 'false'.
    
  - acceptAnyCertificate=boolean	
    Specify 'true' to accept the server certificate without verifying the trust path or 'false' to verify the certificate trust path before accepting the connection.  The default is 'false'.
