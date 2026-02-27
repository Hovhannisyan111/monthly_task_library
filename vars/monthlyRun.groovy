import org.pipeline.MonthlyPipelineManager

def init(def script) {
    return new MonthlyPipelineManager(script)
}

class MonthlyPipelineManager implements Serializable {

    private final def script
    private final FlagFileManager flagManager
    private final int targetDay
    private final int retryIntervalMinutes

    MonthlyPipelineManager(def script) {
        this.script               = script
        this.flagManager          = new FlagFileManager(script)
        this.targetDay            = script.env.MONTHLY_DAY.toInteger()
        this.retryIntervalMinutes = script.env.RETRY_INTERVAL_MIN.toInteger()
    }

    // Is today the monthly run day, or is a retry pending?
    boolean isMonthlyTaskDue() {
        if (flagManager.retryFlagExists()) {
            script.echo 'Retry flag found — monthly task is pending.'
            return true
        }
        def today     = new Date().format('d').toInteger()
        if (today == targetDay) {
            script.echo "Today is day " + today + " — monthly task is due."
            return true
        }
        script.echo "Monthly task not due (today=" + today + ", target=" + targetDay + ")."
        return false
    }

    // Called in post { failure } — only reschedules if it was the monthly day
    void handleFailure() {
        if (isMonthlyTaskDue()) {
            script.echo "Build failed on monthly run day — scheduling retry..."
            flagManager.writeRetryFlag()

            def retryTime = new Date(System.currentTimeMillis() + (retryIntervalMinutes * 60 * 1000))
            def cronExpr  = retryTime.format('m H d M') + ' *'

            script.echo "Retry cron: " + cronExpr
            script.properties([
                script.pipelineTriggers([
                    script.cron(cronExpr)
                ])
            ])
        } else {
            script.echo "Not the monthly run day — no retry scheduled."
        }
    }

    // Called in post { success } — clears everything
    void clearAll() {
        flagManager.clearRetryFlag()
        script.properties([
            script.pipelineTriggers([])
        ])
        script.echo "All flags and triggers cleared."
    }
}
