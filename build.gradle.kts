group = "com.cjcrafter"
version = "1.1.2"

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

// See https://github.com/Minecrell/plugin-yml
bukkit {
    main = "com.cjcrafter.weaponmechanicssignshop.WeaponMechanicsSignShop"
    apiVersion = "1.13"

    authors = listOf("CJCrafter")
    depend = listOf("MechanicsCore", "WeaponMechanics", "Vault")
}

repositories {
    mavenCentral()

    maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    implementation("org.bstats:bstats-bukkit:3.0.0")

    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.cjcrafter:mechanicscore:3.3.0")
    compileOnly("com.cjcrafter:weaponmechanics:3.3.0")

    // Adventure Chat API
    compileOnly("net.kyori:adventure-api:4.15.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.3.2")
    compileOnly("net.kyori:adventure-text-minimessage:4.15.0")
}

tasks.shadowJar {
    archiveFileName.set("WeaponMechanicsSignShop-${project.version}.jar")

    dependencies {
        relocate ("org.bstats", "com.cjcrafter.weaponmechanicssignshop.lib.bstats") {
            include(dependency("org.bstats:"))
        }
    }

    relocate("net.kyori", "me.deecaad.core.lib")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
        options.release.set(16)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
}
