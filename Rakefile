task :default => :test

def lessc(source, target)
  sh "lessc #{source} #{target}"
end

def compress(source, target)
  sh "java -jar 'scripts/yuicompressor-2.4.2.jar' #{source} -v --charset utf-8 -o #{target} 2> /dev/null > /dev/null"
end

desc "Download jstestdriver from web"
task :download do
  unless File.exists? 'scripts/JsTestDriver-1.3.2.jar'
    sh 'wget -O scripts/JsTestDriver-1.3.2.jar http://js-test-driver.googlecode.com/files/JsTestDriver-1.3.2.jar'
  end
end

namespace :js do
  desc 'start jstestdriver srever'
  task :startserver do
    sh 'java -jar scripts/JsTestDriver-1.3.2.jar --port 9876 --browser `which firefox`'
  end

  desc 'Run js unit test against running jstestdriver server'
  task :unit => :download do
    sh 'java -jar scripts/JsTestDriver-1.3.2.jar --tests all --captureConsole'
  end
end
