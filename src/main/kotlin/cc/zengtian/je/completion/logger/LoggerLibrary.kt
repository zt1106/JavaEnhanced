package cc.zengtian.je.completion.logger

enum class LoggerLibrary(val loggerClass: String,
                         val invocationImport: String,
                         val invocation: (String) -> String) {
    LOG4J("org.apache.logging.log4j.Logger",
            "org.apache.logging.log4j.LogManager",
            { clazz -> "LogManager.getLogger($clazz);" });
}