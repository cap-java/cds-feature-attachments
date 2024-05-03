# Demo Application for Attachment Usage

This project is a demo application for attachment usage in CAP Java.
It is based on the [CAP Java Tutorial](https://cap.cloud.sap/docs/java/getting-started)
and extends the tutorial with the usage of attachments.

It is build with [CAP Notebooks](https://cap.cloud.sap/docs/tools/#cap-vscode-notebook).

## Execution

You can download the CAP notebook from the `cap-notebook/attachments-demo-app.capnb` file and run it in Visual Studio
Code with the CAP plugins installed.
It will create a new folder `demoapp` which includes a CAP Java project.

It will also add a UI part.
If the CAP notebook is finished you can start the project with the following commands:

```sh
cd srv
mvn cds:watch
```

After the project is started you can access the application with the following URL:
[http://localhost:8080/](http://localhost:8080/)

## Version

The file `cap-notebook/version.txt` contains the latest version of the project.
The CAP notebook reads the version from the file and uses it in the project.

If the file is not present the version is set to `1.0.0`.
