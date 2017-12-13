# Kotlin Script Object Notation (KSON)

KSON is a Java Script Object Notation library inspired by the
[Java JSON library](https://github.com/stleary/JSON-java)
that is geared more towards [Kotlin](https://kotlinlang.org/)
conventions and features.

Currently the only existing target implementation is for JVM, 
however there is an implementation for targeting JS in the works.

## Goals and Objectives

The goal this library will attempt to accomplish is a null-safe,
easy-to-use, 100% Kotlin implementation for Java Script Object 
Notation (JSON) that promotes dynamic, sleek, and simplistic
conventions and features of the Kotlin programming language.<br>
Additionally, this project will be available as a multi-platform
project with modules targeting both JVM and JS.

## Setup

### Gradle
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

**JVM Target**
```gradle
dependencies {
    compile 'com.github.TheMonitorLizard.KSON:jvm:RELEASE_VERSION'
}
```

**JS Target (Coming Soon)**
```gradle
dependencies {
    compile 'com.github.TheMonitorLizard.KSON:js:RELEASE_VERSION'
}
```

### Maven
```mxml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

**JVM Target**
```mxml
<dependencies>
    <dependency>
        <groupId>com.github.TheMonitorLizard.KSON</groupId>
        <artifactId>jvm</artifactId>
        <version>RELEASE_VERSION</version>
    </dependency>
</dependencies>
```

**JS Target (Coming Soon)**
```mxml
<dependencies>
    <dependency>
        <groupId>com.github.TheMonitorLizard.KSON</groupId>
        <artifactId>js</artifactId>
        <version>RELEASE_VERSION</version>
    </dependency>
</dependencies>
```