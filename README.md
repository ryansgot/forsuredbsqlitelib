# forsuredbsqlitelib
Current build status: [![CircleCI](https://circleci.com/gh/ryansgot/forsuredbsqlitelib/tree/master.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/ryansgot/forsuredbsqlitelib/tree/master)


## Revisions

###
- Support for composite keys
- Support for default value
- Removed dependency upon google guava

### 0.4.0
- Integration with forsuredbapi-0.9.0--specifically regarding the ```DBMSIntegrator``` interface.

### 0.3.0
- Support for index creation for columns that are not uniuqe
- Fix new column creation bug where uniqueness was not considered
