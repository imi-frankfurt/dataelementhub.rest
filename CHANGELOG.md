# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0] - 2022-11-03
### Added
- namespace admins can invite users to their namespace(s) and manage the access scopes [[#15](https://github.com/mig-frankfurt/dataelementhub.rest/issues/15)]
  - this allows other people to see hidden namespaces or to create elements in other peoples namespaces (depending on the granted access level)
- export feature [[#17](https://github.com/mig-frankfurt/dataelementhub.rest/issues/17)]
  - allow users to export namespaces (or separate elements) to JSON or XML files
- import feature [[#20](https://github.com/mig-frankfurt/dataelementhub.rest/issues/20)]
  - allow users to import multiple elements or even namespaces at once, given the respective format (see export)
- Endpoint to return all available paths for specified element  [[#36](https://github.com/mig-frankfurt/dataelementhub.rest/issues/36)]
  - Elements can be contained in multiple groups or records. This endpoint returns a list of all paths to the given element
- Endpoint to update the members of dataElementGroup/record [[#26](https://github.com/mig-frankfurt/dataelementhub.rest/issues/26)]
- DeepLinking for swagger [[#82](https://github.com/mig-frankfurt/dataelementhub.rest/issues/82)]
### Changed
- ElementController does not process namespaces anymore [[#38](https://github.com/mig-frankfurt/dataelementhub.rest/issues/38)]
  - since the endpoints to handle namespaces and other elements are separated, this is also split into different controllers now
- updated dehub-dal and dehub-model
  - dehub.model was updated to release 2.1.0, see [dehub.model changelog](https://github.com/mig-frankfurt/dataelementhub.model/blob/master/CHANGELOG.md) for changes
  - dehub.dal was updated to release 3.0.0, see [dehub.dal changelog](https://github.com/mig-frankfurt/dataelementhub.dal/blob/master/CHANGELOG.md) for changes
- The way jooq is used was changed [[#73](https://github.com/mig-frankfurt/dataelementhub.rest/issues/73)]
  - the jooq dsl context is now autowired instead of getting it from dehub-dal
### Fixed
- No longer allow users without the respective keycloak role to create namespaces [[#34](https://github.com/mig-frankfurt/dataelementhub.rest/issues/34)]
### Security
- Update spring boot to 2.7.5

## [1.2.2] - 2021-12-14
### Security
- remove log4j dependency

## [1.2.1] - 2021-12-09
### Fixed
- update dehub-model to fix bug when retrieving namespace members
  - dehub.model was updated to release 1.2.1, due to [dehub.model #15](https://github.com/mig-frankfurt/dataelementhub.model/issues/15)

## [1.2.0] - 2021-11-24
### Added
- retrieving namespace members can optionally exclude elements which are also in a group [[#16](https://github.com/mig-frankfurt/dataelementhub.rest/issues/16)]
  - when trying to create a hierarchical view of the namespace members in a gui, it might be beneficial to only retrieve those namespace members that are not in a group, so that they can be lazily loaded and displayed in the gui when opening the group
### Changed
- updated dehub-model, jooq, gson and springdoc-openapi-ui
  - dehub.model was updated to release 1.2.0, see [dehub.model changelog](https://github.com/mig-frankfurt/dataelementhub.model/blob/master/CHANGELOG.md) for changes

## [1.1.0] - 2021-10-12
### Changed
- updated dehub.model
  - dehub.model was updated to release 1.1.0, see [dehub.model changelog](https://github.com/mig-frankfurt/dataelementhub.model/blob/master/CHANGELOG.md) for changes
### Fixed
- openapi urls fixed [[#3](https://github.com/mig-frankfurt/dataelementhub.rest/issues/3)]
  - the used urls were outdated and therefore not working as intended
### Security
- spring security updated to 5.5.2 [[#6](https://github.com/mig-frankfurt/dataelementhub.rest/issues/6)]

## [1.0.1] - 2021-10-04
### Fixed
- x-forwarded-proto header and host header are optional [[#1](https://github.com/mig-frankfurt/dataelementhub.rest/issues/1)]
  - when the dehub-rest application is not running behind a reverse proxy, those parameters are not set, so having them as mandatory was not correct
### Security
- update commons.fileupload due to [CVE-2016-1000031](https://github.com/advisories/GHSA-7x9j-7223-rg5m)

## [1.0.0] - 2021-09-24
### Added
- initial version
