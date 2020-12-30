package com.sbss.bithon.agent.bootstrap;

public interface Constant {

    enum VMOption {
        JAVA_CLASS_PATH("java.class.path"),
        CATALINA_HOME("catalina.home");

        private final String value;

        VMOption(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    enum ProgramOption {
        LOGGING_CONFIG("logging.config");

        private final String value;

        ProgramOption(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    enum ManifestAttribute {
        PLUGIN_CLASS("Plugin-Class"),
        SPRING_BOOT_CLASSES("Spring-Boot-Classes");

        private final String value;

        ManifestAttribute(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    enum Suffix {
        JAR(".jar");

        private final String value;

        Suffix(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
