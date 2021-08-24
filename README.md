# clientarguments
Quality of life library for client-sided Minecraft Fabric command building.

## Installation
Replace `${version}` with the artifact version.
### Gradle
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
dependencies {
    modImplementation 'dev.xpple:clientarguments:${version}'
}
```
If you are having trouble installing the package, it might be because the environment variables for GitHub aren't setup 
correctly. This is likely the case if you get this error message: `Received status code 400 from server: Bad Request`. 
You may fix this either by manually adding the environment variables, or by modifying your build script slightly. 
Firstly, add an `.env` file in your project's root directory and add these lines, replacing `<username>` with your 
GitHub username and `<token>` with your GitHub token (which must have the permission `read:packages`):
```.env
USERNAME=<username>
TOKEN=<token>
```
If you are using git, **make sure to add this file to `.gitignore`**.

Next, add this function anywhere in your `build.gradle` file.
```gradle
def static getenv(path = ".env") {
    def env = [:]

    def file = new File(path)
    if (file.exists()) {
        file.eachLine { line ->
            def (name, value) = line.tokenize("=")
            env[name.trim()] = value.trim()
        }
    }
    return env
}
```
Lastly, change the credentials block to:
```gradle
credentials {
    def envs = getenv()
    username = project.findProperty("gpr.user") ?: envs["USERNAME"] ?: System.getenv("USERNAME")
    password = project.findProperty("gpr.key") ?: envs["TOKEN"] ?: System.getenv("TOKEN")
}
```
