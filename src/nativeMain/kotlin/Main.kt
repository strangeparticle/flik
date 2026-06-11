import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

class Flik : CliktCommand(name = "flik") {
    override fun run() = Unit
}

class Hello : CliktCommand(name = "hello") {
    private val name: String by option(help = "Name to greet").default("world")

    override fun run() {
        echo(greeting(name))
    }
}

fun main(args: Array<String>) = Flik().subcommands(Hello()).main(args)
