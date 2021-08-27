# clientarguments
Quality of life library for client-sided Minecraft Fabric command building.

## Installation
Replace `${version}` with the artifact version.
### Gradle
You may choose between my own maven repository and GitHub's package repository.
#### My own
```gradle
repositories {
    maven {
        url 'https://maven.xpple.dev/maven2'
    }
}
```
#### GitHub packages
```gradle
repositories {
    maven {
        url 'https://maven.pkg.github.com/xpple/clientarguments'
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}
```
Import it:
```
dependencies {
    modImplementation 'dev.xpple:clientarguments:${version}'
}
```
Note: if you choose to use GitHub packages and get `Received status code 400 from server: Bad Request`, you need to 
configure your environment variables for GitHub.
