![java 17](https://img.shields.io/badge/java-17-brightgreen.svg)
---
<p align="center">
	<img src="https://FoelliX.github.io/AQL-System/logo.png" width="299px" /><br />
	<font style="color: #FFFFFF; font-size: 50px; background: #3c6708; border-top: 17px solid #3c6708; border-radius: 0px 0px 7px 7px; border-left: 20px solid #3c6708; border-right: 20px solid #3c6708; margin-left: -1px;">WebService</font>
</p>

# AQL-WebService
The *Android App Analysis Query Language WebService (AQL-WebService)* is a wrapper for [AQL-Systems](https://github.com/FoelliX/AQL-System).
It makes an AQL-System accessible over the Internet by allowing access through an REST API.
In this regard, networks of analysis tools can be build by coupling one or more frontends with arbitrary many AQL-WebServices.
AQL-WebServices can also be chained as indicated in the figure below.
One example for such a analysis tool network is known as [CoDiDroid](https://github.com/FoelliX/CoDiDroid).

<p align="center">
	<img src="overview.png" width="400px" style="border-radius:15px" />
</p>

## Example
- Any AQL-System wrapped in an AQL-WebService can be configured the usual way (See [configuration tutorial](https://github.com/FoelliX/AQL-System/wiki/Configuration)).
To access the tools configured in an AQL-WebService the configuration associated with the frontend has to specify the webservice as an external tool as explained in the tutorial.
- To access an existing AQL-WebService follow the AQL-System's [video tutorials](https://github.com/FoelliX/AQL-System/wiki/Video_tutorials).

## Configuration, Launch Parameters and Runtime Commands
Any AQL-WebService must be configured by the parameters in its `config.properties` file.
The file will be created with default values if it does not exist.
The following table explains each of the configurable parameters:

| Entry | Meaning |
| ----- | ------- |
| `aqlPath = /path/to/aql` | Defines the path to the directory where the underlying AQL-System's storage or temp files etc. should be stored. |
| `timeout = 20m` | Defines how long to wait for an answer. Three different units can be used (`s`: seconds, `m`: minutes, `h`: hours). |
| `queueStart = 10000` | Determines the first id that will be associated with the first query received. Thereafter it is increased with each query. |
| `allowedURLs = http://localhost` | Defines which remote host are allowed to issue queries to this webservice (Separator: `,`). |
| `port = 8080` | Definition of the port to listen on. |
| `bufferTime = 1000` | Time in milliseconds to collect incoming queries before transferring them to the underlying AQL-System. Required to allow parallel question answering. |
| `asyncEnabled = false` | Specifies whether the asynchronous mode is enabled or not (required by [AQL-Online](https://github.com/FoelliX/AQL-Online)). |
| `keystorePath` | To activate SSL encryption please provide a path to the keystore ([help](https://stackoverflow.com/questions/906402/how-to-import-an-existing-x-509-certificate-and-private-key-in-java-keystore-to)). |
| `keystorePass` | Provide the password associated with the keystore. |
| `truststorePath` | To activate SSL encryption please provide a path to the truststore ([help](https://stackoverflow.com/questions/906402/how-to-import-an-existing-x-509-certificate-and-private-key-in-java-keystore-to) - most of the time identical with `keystorePath`). |
| `truststorePass` | Provide the password associated with the truststore. |

Who is allowed to access a WebService is specified in the `accounts.csv` file.
The following table shows an example of the content of such a file.
If the file does not exist it will be created holding only the last line of the following example.

| username | password | maxPerDay | maxQuestionsPerQuery | timestamp | counter |
| -------- | -------- | --------- | -------------------- | --------- | ------- |
| free[111.222.333.444] | | 5 | 3 | 1621401457241 | 2 |
| free | | 5 | 3 | 0 | 0 |
| aql | 8ab8ea678... | 0 | 0 | 1621401458360 | 18 |

The last line represents an account with the username `aql` and password `S3cR3T!` (stored as SHA256-hash).
The line starting with `free` signals that the webservice can be accessed without an existing account.
Lines such as `free[111.222.333.444]` are automatically added once a user accesses the webservice via the free account.
From its first access the user will be remembered by its IP.
The columns `maxPerDay` (maximal number of queries per day) and `maxQuestionsPerQuery` (maximal number of questions in each query asked) define the limits of a user.
The `timestamp` and `counter` column denote the time of the first access during the last 24 hours and how many queries have been scheduled since then.

Once an AQL-WebService is configured, certain launch parameters can be used to further influence its execution.
The table below explains the available launch parameters:

| Parameter	 | Meaning |
| ---------- | ------- |
| `-config "X"`, `-cfg "X"`, `-c "X"` | By default the `config.xml` file in the tool’s directory is used as configuration. With this parameter a different configuration file can be chosen. `X` has to reference the path to and the configuration file itself. |
| `-rules "X"` | By default the rule-set in rules.xml file is loaded. With this parameter a different rule file can be chosen. `X` has to reference the path to and the rule file itself. |
| `-debug "X"`, `-d "X"` | The output generated during the execution of this tool can be set to different levels. `X` may be set to: `error`, `warning`, `normal`, `debug`, `detailed` (ascending precision from left to right). Additionally it can be set to `short`, the output will then be equal to `normal` but shorter at some points. By default it is set to `normal`. |
| `-backup`, `-b` | When this launch parameter is provided, the current storage of the underlying AQL-System is backed up on start. |
| `-reset`, `-r` | By this parameter the storage of the underlying AQL-System is resetted on start. Whenever a backup is scheduled as well, it will be generated before the reset. |

When the AQL-WebService is up and running the following commands can be used to perform the associated actions:

| Command | Action |
| ------- | ------ |
| `help` | Shows a list of available commands and other helpful information. |
| `exit`, `quit` | To stop the webservice. |
| `user %NAME% %PASSWORD%` | Initializes a new user (e.g. `user FoelliX password123`). |
| `port %PORT%` | The port, to which the webservice is listening to, is changed to %PORT% (e.g. `port 8081`). |
| `info` | Shows some basic information. |
| `tools` | Shows a list of all tools in the current config. |
| `accounts` | Shows a table of all accounts (To edit accounts please edit `accounts.csv`). |
| `tasks` | Shows the current execution task tree. |
| `backup` | Backups the underlying AQL-System's storage. |
| `reset` | Resets the underlying AQL-System. |

## Publications
- *Together Strong: Cooperative Android App Analysis* (Felix Pauck, Heike Wehrheim)  
ESEC/FSE 2019 [https://dl.acm.org/citation.cfm?id=3338915](https://dl.acm.org/citation.cfm?id=3338915)

## License
The AQL-WebService is licensed under the *GNU General Public License v3* (see [LICENSE](https://github.com/FoelliX/AQL-WebService/blob/master/LICENSE)).

## Contact
**Felix Pauck** (FoelliX)  
Paderborn University  
fpauck@mail.uni-paderborn.de  
[http://www.FelixPauck.de](http://www.FelixPauck.de)

## Links
- CoDiDroid, an analysis tool network build out of AQL-WebServices: [https://github.com/FoelliX/CoDiDroid](https://github.com/FoelliX/CoDiDroid)
- AQL-System, the system wrapped in AQL-WebServices, which can also be used as a frontend interface: [https://github.com/FoelliX/AQL-System](https://github.com/FoelliX/AQL-System)
- BREW, a frontend implementation for benchmark execution: [https://github.com/FoelliX/BREW](https://github.com/FoelliX/BREW)
- [AQL-Online](https://github.com/FoelliX/AQL-Online), a website which can be used as frontend, too.