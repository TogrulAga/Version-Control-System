package svcs

import java.io.File
import java.security.MessageDigest

fun help() {
    println(
        """These are SVCS commands:
config     Get and set a username.
add        Add a file to the index.
log        Show commit logs.
commit     Save changes.
checkout   Restore a file.
           """.trimIndent()
    )
}

fun config(args: Array<String>) {
    val configFile = File("vcs" + File.separator + "config.txt")

    if (args.size == 2) {
        if (!configFile.exists()) configFile.createNewFile()
        configFile.writeText(args[1])
        println("The username is ${args[1]}.")
    } else {
        if (configFile.exists()) {
            println("The username is ${configFile.readText()}.")
        } else {
            println("Please, tell me who you are.")
        }
    }
}

fun add(args: Array<String>) {
    val indexFile = File("vcs" + File.separator + "index.txt")
    if (args.size == 1) {
        if (indexFile.exists()) {
            println("Tracked files:")
            indexFile.readLines().forEach {
                println(it)
            }
        } else {
            println("Add a file to the index.")
        }
    } else if (args.size == 2) {
        val fileToTrack = File(args[1])
        if (fileToTrack.exists()) {
            println("The file '${args[1]}' is tracked.")
            indexFile.appendText("${args[1]}\n")
        } else {
            println("Can't find '${args[1]}'.")
        }
    } else {
        println("Too many arguments.")
    }
}

fun log() {
    val log = File("vcs" + File.separator + "log.txt")
    if (!log.exists() || log.readLines().isEmpty()) {
        println("No commits yet.")
        return
    }

    println(log.readText())
}

fun commit(args: Array<String>) {
    if (args.size == 1) {
        println("Message was not passed.")
        return
    }

    val log = File("vcs" + File.separator + "log.txt")
    val index = File("vcs" + File.separator + "index.txt")
    val commits = File("vcs" + File.separator + "commits")
    val config = File("vcs" + File.separator + "config.txt")
    if (!index.exists() || index.readLines().isEmpty()) {
        println("Nothing to commit.")
        return
    }

    val commitMessage = args[1]
    val bytes = mutableListOf<Byte>()
    index.readLines().forEach { line ->
        bytes.addAll(File(line).readBytes().toMutableList())
    }
    val hash = StringBuilder()
    MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()).forEach {
        hash.append(String.format("%02x", it))
    }

    if (hash.toString() in commits.list()) {
        println("Nothing to commit.")
        return
    }

    val logContent = log.readLines().filter { it.isNotEmpty() }.toMutableList()

    logContent.add(0, commitMessage)
    logContent.add(0, "Author: ${config.readText()}")
    logContent.add(0, "commit $hash")
    log.writeText("")
    for ((i, line) in logContent.withIndex()) {
        log.appendText(line + "\n")
        if ((i + 1) % 3 == 0 && i != logContent.lastIndex) {
            log.appendText("\n")
        }
    }

    val commitFolder = File("vcs" + File.separator + "commits" + File.separator + hash)
    commitFolder.mkdir()
    index.readLines().forEach {
        File(it).copyTo(File(commitFolder, it))
    }
    println("Changes are committed.")
}

fun checkout(args: Array<String>) {
    if (args.size == 1) {
        println("Commit id was not passed.")
        return
    }

    val commits = File("vcs" + File.separator + "commits").list()

    if (args[1] !in commits) {
        println("Commit does not exist.")
        return
    }

    val commitFolder = File("vcs" + File.separator + "commits" + File.separator + args[1])
    for (file in commitFolder.listFiles()) {
        file.copyTo(File(file.name), true)
    }

    println("Switched to commit ${args[1]}.")
}

fun main(args: Array<String>) {
    val wd = System.getProperty("user.dir")
    val vcs = File(wd + File.separator + "vcs")
    if (!vcs.exists()) vcs.mkdir()
    val commits = File("vcs" + File.separator + "commits")
    if (!commits.exists()) commits.mkdir()
    val log = File("vcs" + File.separator + "log.txt")
    if (!log.exists()) log.createNewFile()

    if (args.isEmpty()) {
        help()
    } else if (args.isNotEmpty()) {
        when (args[0]) {
            "--help" -> help()
            "config" -> config(args)
            "add" -> add(args)
            "log" -> log()
            "commit" -> commit(args)
            "checkout" -> checkout(args)
            else -> println("'${args[0]}' is not a SVCS command.")
        }
    }
}