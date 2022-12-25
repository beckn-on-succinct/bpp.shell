# BPP-Shell
An open source [beckn](https://becknprotocol.io) adaptor  for the [Succinct web framework](https://succinct.in).

## Introduction

This is used in conjunction with ecommerce adaptor applications such as 

1. [bpp.woocommerce](https://github.com/venkatramanm/bpp.woocommerce) 
2. [bpp.shopify - under development](https://github.com/venkatramanm/bpp.shopify) 
3. [bpp.openkart- under development](https://github.com/venkatramanm/bpp.openkart)

### Creating an application that uses an ecommerce adpator.
```	
e.g 
mvn archetype:generate \
-DarchetypeGroupId=com.github.venkatramanm.swf-all \
-DarchetypeArtifactId=swf-bpp-archetype \
-DarchetypeVersion=1.0-SNAPSHOT \
-DgroupId=in.succinct \
-DartifactId=woocommerce.app -Dadaptor=bpp.woocommerce -Dversion=1.0-SNAPSHOT
```
Refer   [bpp.woocommerce](https://github.com/venkatramanm/bpp.woocommerce) for additional details about configuring your applicaion.


## My ECommerce App doesnot have an adaptor?
Steps to convert your ecommerce platform into to a beckn enabled provider platform 

*  Create an adaptor scaffold 

```
e.g.
mvn archetype:generate \
-DarchetypeGroupId=com.github.venkatramanm.swf-all \
-DarchetypeArtifactId=swf-bpp-adaptor-archetype \
-DarchetypeVersion=1.0-SNAPSHOT \
-DgroupId=in.succinct \
-DartifactId=bpp.shopify \
-Dversion=1.0-SNAPSHOT
	
```
	
* Files to be changed .
   * ECommerceAdaptor.java, WebHook.java 
