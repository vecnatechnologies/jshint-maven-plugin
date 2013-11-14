def buildLog = new File(basedir, 'build.log')
assert buildLog.exists()
assert buildLog.text.contains('violations: 6')

def report = new File(basedir, 'target/jshint.xml')
assert report.exists()

def reportDoc = new XmlParser().parse(report)
def files = reportDoc.file

assert 2 == files.size()
assert 6 == files.error.size()
