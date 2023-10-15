# ISB CA 

The ISB CA is a separately maintained fork of the EJBCA Community CA maintanined by IS Blocks Ltd

The recommended stack for the ISB CA is: 
* JDK 17
* Ubuntu 18, 20 or 22
* Maria DB
* KeyCloak 22

In addition to the rich community features the ISB CA offers the following: 
* External VA support through ISB Apache Kafka plugin 
* Support for ED25519 algorithms 
* Integrity protected audit logging

## Get started

To get started with **ISB CA**, clone **[isblocks-java-pki](https://github.com/ISBlocksltd/isblocks-java-pki.git)** and install it, see **[EJBCA Installation](https://doc.primekey.com/ejbca/ejbca-installation)**.


In addition to the instructions above, the EJBCA_HOSTNAME and EJBCA_PORTNUMBER are requried variables 

For example:

EJBCA_HOSTNAME=isbca01.isblocks.com
EJBCA_PORT=443

Where the EJBCA_HOSTNAME represents the actual FQDN where the CA can be reached and the EJBCA_PORTNUMBER represents the HTTPS port number. 

## Commercial Support
Commercial support is available from IS Blocks Ltd. Contact us for more details

## License
ISB CA is licensed under the LGPL license and thus compliant with the original licence as published by Keyfactor, please see **[LICENSE](LICENSE)**.

## Disclaimer
IS Blocks does not have any relationship with Keyfactor
