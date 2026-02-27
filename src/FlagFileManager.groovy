package org.pipeline

class FlagFileManager implements Serializable {

    private final def script
    private final String basePath

    FlagFileManager(def script) {
        this.script   = script
        this.basePath = script.env.JENKINS_HOME + '/jobs/' + script.env.JOB_NAME
    }

    String retryFlagPath() {
        return basePath + '/monthly_retry.flag'
    }

    boolean retryFlagExists() {
        return script.fileExists(retryFlagPath())
    }

    void writeRetryFlag() {
        def f = retryFlagPath()
        script.sh "mkdir -p \"\$(dirname '" + f + "')\" && touch '" + f + "'"
    }

    void clearRetryFlag() {
        def f = retryFlagPath()
        if (script.fileExists(f)) {
            script.sh "rm -f '" + f + "'"
        }
    }
}
