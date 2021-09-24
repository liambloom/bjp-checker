# Checker for BJP

This is a third-party checker program meant to check the exercises and programming projects from the [Building Java Programs](https://www.buildingjavaprograms.com/) series. It comes with checks for the 3rd edition, but it is possible for users to add other checks, such as those for future editions.

### Modules

This program is split into 3 modules:
- Annotations &mdash; This module only include the annotations used to annotate code so that the checker can find the exercises and programming projects in it. None of the logic is in this module.
- API &mdash; This module contains all the internal logic that loads, filters, and runs the tests and code to be tested. It also imports the annotations module.
- UI &mdash; This module contains all the code for the CLI and GUI, as well as importing the API module.

### CLI Usage

This section documents the commands that you can use in the checker

##### 