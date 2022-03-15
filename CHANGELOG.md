# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - UNRELEASED
### Added
- export feature
- Endpoint to enable importing elements to a staging area
- Endpoint to retrieve all imports
- Endpoint to get import status by ID
- Endpoint to get all import members
- Endpoint to get an imported element by ID
- Endpoint to get all members of a stagedElement by ID
- Endpoint to enable converting the stagedElements to drafts
- Endpoint to delete an import by ID
- Endpoint to return all available paths for specified element
- Endpoint to update the members of dataElementGroup/record
### Changed
- ElementController does not process namespaces anymore 
### Deprecated
### Removed
### Fixed
### Security

## [1.2.2] - 2021-12-14
### Security
- remove log4j dependency

## [1.2.1] - 2021-12-09
### Fixed
- update dehub-model to fix bug when retrieving namespace members

## [1.2.0] - 2021-11-24
### Added
- retrieving namespace members can optionally exclude elements which are also in a group
### Changed
- updated dehub-model, jooq, gson and springdoc-openapi-ui

## [1.1.0] - 2021-10-12
### Changed
- updated dehub.model
### Fixed
- openapi urls fixed
### Security
- spring security updated to 5.5.2

## [1.0.1] - 2021-10-04
### Fixed
- x-forwarded-proto header and host header are optional
### Security
- update commons.fileupload due to [CVE-2016-1000031](https://github.com/advisories/GHSA-7x9j-7223-rg5m)

## [1.0.0] - 2021-09-24
### Added
- initial version
