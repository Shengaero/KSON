# Kotlin Script Object Notation (KSON)

KSON is a completely rewritten port of the [Java JSON library](https://github.com/stleary/JSON-java)
that is geared more towards [Kotlin](https://kotlinlang.org/).

## Goals and Objectives

The goal this library is attempting to accomplish is a null-safe,
easy-to-use, 100% Kotlin wrapper for Java Script Object Notation (JSON),
as well as allowing those currently using `org.json` in their Kotlin or
Java based projects the ability to swap out `org.json` for this with
very little to no breaking changes to their code.

Currently work is mostly being put towards **KSONArray implementation**
and **Testing current releases of KSONObject**, however there are some
more experimental features which will be added in later stages of the
library's development.

## Setup

**Gradle**
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

```gradle
dependencies {
    compile 'com.github.TheMonitorLizard:KSON:master-SNAPSHOT'
}
```

**Maven**
```mxml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```mxml
<dependencies>
    <dependency>
        <groupId>com.github.TheMonitorLizard</groupId>
        <artifactId>KSON</artifactId>
        <version>master-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Features and Examples

KSON has several different kinds of **syntactic sugar** using operator functions
that makes heavy JSON code look more readable.

```kotlin
fun main(args: Array<String>) {
    val kson = KSONObject()
    
    kson["flavor"]    = "cherry"
    kson["cone"]      = "waffle"
    kson["sprinkles"] = true
    kson["price"]     = 3.49
    
    println("That will be $${kson["price"]}.") // "That will be $3.49."
}
```

KSON allows for annotated objects to be wrapped directly into a
`KSONObject` or a `KSONArray`.

```kotlin
fun main(args: Array<String>) {
    val me = Person("Kaidan", 18)   // Annotations on 'Person' will allow KSON
    val kson = KSONObject(me)       // to wrap and serialize the object into a KSONObject
    
    println(kson["introduction"])   // "Hi, my name is Kaidan, and I am 18 years old!"
    
    kson["name"] = "Shengaero"      // KSONObjects can also deserialize objects
    kson["age"] = 17                // based on the data they contain.
    
    val someone = kson.construct<Person>()
    
    println(someone.introduction()) // "Hi, my name is Shengaero, and I am 17 years old!"
}

@KSON
class Person @KSONConstructor("name", "age")
constructor(
    @property:KSONValue("name") val name: String,
    @property:KSONValue("age") val age: Int
) {
    @KSONValue("introduction")
    fun introduction() = "Hi, my name is $name, and I am $age years old!"
}
```

Have a big project and need to smoothly transition to KSON?
If you keep `org.json` in your project's classpath, you can use one of the
conversion functions in `me.kgustave.kson.json` to do a one-step conversion:

```kotlin
fun main(args: Array<String>) {
    val json = JSONObject()
    
    json.put("height", 72.8)
    json.put("weight", 124.9)
    json.put("age", 26)
    
    println(json.toString(2))
}
```
... getting to KSON from here is a breeze...
```kotlin
fun main(args: Array<String>) {
    val json = JSONObject()
    
    json.put("height", 72.8)
    json.put("weight", 124.9)
    json.put("age", 26)
    
    println(json.kson.toString(2)) // One extension's all it takes!
}
```

Working on a project that uses Java *and* Kotlin?
KSON is **100%** interoperable with Java code.

```java
public class Main {
    public static void main(String[] args){
        KSONObject kson = new KSONObject();
      
        kson.put("java", true);
        kson.put("kotlin", true);
      
        boolean javaWorks = kson.get("java");
        boolean kotlinWorks = kson.get("kotlin");
      
        if(javaWorks && kotlinWorks) {
            System.out.println("It's perfectly fine to use in java code!");
        }
    }
}
```