[ ![Build Status] [travis-image] ] [travis] [ ![Release] [release-image] ] [releases] [ ![License] [license-image] ] [license]

## Overview

Extracts campaign data from marketing channels and stores in Redshift. Supports the following marketing channels:

* **[Singular] [singular]**

Used with **[Snowplow] [snowplow]** for marketing attribution.

## User Quickstart

Assuming Java 7 or higher installed:

```bash
> wget http://dl.bintray.com/snowplow/snowplow-generic/huskimo_0.1.0.zip
> unzip huskimo_0.1.0.zip
> vi config.yml
> ./huskimo-0.1.0 --config config.yml
```

## Developer Quickstart

### Building

Assuming git, **[Vagrant] [vagrant-install]** and **[VirtualBox] [virtualbox-install]** installed:

```bash
 host> git clone https://github.com/snowplow/huskimo
 host> cd huskimo
 host> vagrant up && vagrant ssh
guest> cd /vagrant
guest> sbt compile
```

## Copyright and license

Huskimo is copyright 2015 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0] [license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[travis]: https://travis-ci.org/snowplow/huskimo
[travis-image]: https://travis-ci.org/snowplow/huskimo.png?branch=master

[release-image]: http://img.shields.io/badge/release-0.1.0-blue.svg?style=flat
[releases]: https://github.com/snowplow/huskimo/releases

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

[singular]: https://www.singular.net

[snowplow]: https://github.com/snowplow/snowplow

[vagrant-install]: http://docs.vagrantup.com/v2/installation/index.html
[virtualbox-install]: https://www.virtualbox.org/wiki/Downloads
