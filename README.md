# BPP-Shell
An open source [beckn](https://becknprotocol.io) adaptor plugin for the [Succinct web framework](https://succinct.in).

## Introduction

This is a plugin used in conjunction with ecommerce adaptor applications such as 

1. [bpp.woocommerce](https://github.com/venkatramanm/bpp.woocommerce) 
2. [bpp.shopify - under development](https://github.com/venkatramanm/bpp.shopify) 
3. [bpp.openkart- under development](https://github.com/venkatramanm/bpp.openkart)


## My ECommerce App doesnot have an adaptor?
Steps to convert your ecommerce platform into to a beckn enabled provider platform 

*  Create the application scaffold 

mvn archetype:generate -DarchetypeGroupId=com.github.venkatramanm.swf-all  -DarchetypeArtifactId=swf-bpp-archetype -DarchetypeVersion=1.0-SNAPSHOT -DgroupId=your_application_group_id -DartifactId=your_artifact_id -Dversion=1.0-SNAPSHOT

```	
e.g 
mvn archetype:generate \
-DarchetypeGroupId=com.github.venkatramanm.swf-all \
-DarchetypeArtifactId=swf-bpp-archetype \
-DarchetypeVersion=1.0-SNAPSHOT \
-DgroupId=in.succinct \
-DartifactId=woocommerce.app -Dadaptor=bpp.woocommerce -Dversion=1.0-SNAPSHOT
```
	
* Locate ECommerceExtension.java and fill in the stubs
* Edit  overrideProperties/config/swf.properties and point swf.host etc to the correct domain/subdomain where this server would be hosted.
 
``` 
#Point to your public url 
#swf.host=your_fully_qualified_domain
#swf.external.port=443
#swf.external.scheme=https
...
#swf.encryption.support=true
#swf.key.store.directory=./.keystore
#swf.key.store.password=mypassword
#swf.key.entry.succinct.password=myentrypassword

```
* Start application from the project root folder using bin/swfstart
* goto https://your_domain and login as root and root, 
* change the password to something difficult to guess.
* By default, you will be onboarded to https://registry.becknprotocol.io/subscribers


* If you wish to register to a different registry like ondc, you will need to follow their instructions. 
* To get your subscription information, by visiting the url,
```
   https://your_domain/bpp/subscriber_json
```
