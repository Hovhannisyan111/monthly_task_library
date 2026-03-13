def isMonthlyTaskDue(def script) {
    def doneF     = flagPath(script, 'done')
    def thisMonth = new Date().format('MM-yyyy')

    if (script.fileExists(doneF) && script.readFile(doneF).trim() == thisMonth) {
        script.echo 'Already ran this month — skipping.'
        return false
    }
    def today     = new Date().format('d').toInteger()
    def targetDay = script.env.MONTHLY_DAY.toInteger()
    return today >= targetDay
}

def onSuccess(def script) {
    script.sh "echo '" + new Date().format('MM-yyyy') + "' > '" + flagPath(script, 'done') + "'"
}

def onFailure(def script) {
    script.echo 'Monthly task failed — will retry on next build.'
}

def flagPath(def script, String type) {
    return script.env.JENKINS_HOME + '/jobs/' + script.env.JOB_NAME + '/monthly_' + type + '.flag'
}
