# DataElementHub REST

This iteration of DataElementHub Rest is based on the fundamental concepts of
the [Samply.MDR](https://bitbucket.org/medicalinformatics/mig.samply.mdr.gui). Based on the
experience gained, the concept was revised and redeveloped, with particular emphasis on the
separation of the backend and frontend.
DataElementHub Rest is the next evolution of [MIG.MDR.rest](https://github.com/mig-frankfurt/mdr.rest), introducing
a rebranding.

Please see the [DataElementHub](https://dataelementhub.de/) website for further information.

DataElementHub REST provides a RESTful API to the DataElementHub. It can either be used directly
via REST calls or from a GUI (reference implementation tbd). To see the API documentation as well as
examples, check out the swagger documentation as listed below in `Usage`.

# Environment Variables

```
ISSUER_URI=https://yourkeycloakhost/auth/realms/YOURREALM
JWK_SET_URI=https://yourkeycloakhost/auth/realms/YOURREALM/protocol/openid-connect/certs
DB_HOST=your.db.host
DB_NAME=your.db.name
DB_USER_NAME=your.db.username
DB_USER_PW=your.db.password
```

## Usage

The Swagger documentation of the REST interface is available at
`http://localhost:8090/swagger-ui.html`.

## Build

Use maven to build the `war` file:

```
mvn clean package
```