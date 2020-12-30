package com.sbss.bithon.agent.bootstrap;

public interface Constant {

    enum VMOption {
        JAVA_CLASS_PATH("java.class.path"),
        CONF("conf"),
        CATALINA_HOME("catalina.home");

        private String value;

        VMOption(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    enum ProgramOption {
        LOGGING_CONFIG("logging.config");

        private String value;

        ProgramOption(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    enum ManifestAttribute {
        TRANSFORMER_CLASS("Transformer-Class"),
        SPRING_BOOT_CLASSES("Spring-Boot-Classes");

        private String value;

        ManifestAttribute(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    enum Suffix {
        JAR(".jar");

        private String value;

        Suffix(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
