# ISB CA 

The ISB CA is a separately maintained fork of the EJBCA Community CA maintanined by IS Blocks Ltd

EJBCA is developed in Java and runs on a JVM such as OpenJDK, available on most platforms such as Linux and Windows although the recommended stack for the ISB CA is
* JDK 17
* Ubuntu 18, 20 or 22
* Maria DB
* KeyCloak 22

## Get started

To get started with **ISB CA**, clone **[isblocks-java-pki]()** and install it, see **[EJBCA Installation](https://doc.primekey.com/ejbca/ejbca-installation)**.

In addition to the instructions above, it is required to export the EJBCA_HOSTNAME and the EJBCA_PORTNUMBER variable on the Linux system.

For example:

EJBCA_HOSTNAME=isbca01.isblocks.com

EJBCA_PORT_NUMBER=443

Where the HOSTNAME represents the actual FQDN where the CA can be reached and EJBCA_PORT_NUMBER represents the https port number without certificate authentication (default 8442)


## Commercial Support
Commercial support is available from IS Blocks Ltd. Contact us for more details at support@isblocks.com

## License
ISB CA is licensed under the LGPL license and thus compliant with the original licence as published by Keyfactor, please see **[LICENSE](LICENSE)**.

## Disclaimer
IS Blocks does not have any relationship with Keyfactor
