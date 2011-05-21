require 'rake/clean'
require 'tempfile'
task :default => :test

def lessc(source, target)
  sh "lessc #{source} #{target}"
end

def compress(type, source, target)
  sh "java -jar 'scripts/yuicompressor-2.4.2.jar' --type #{type} " +
    "--charset utf-8 -o #{target} \"#{source}\" 2> /dev/null > /dev/null"
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

namespace :css do
  desc 'Compile less , Compress css'
  task :compile do
    less = FileList['less/**/*.less'].exclude('less/**/*.inc.less')

    less.each do |source|
      target = source.sub(/less$/, 'css').sub(/^less/, 'public/css')
      lessc source, target
    end
   end
end

namespace :html do
  CLEAN.include('src/templates')

  def get_dir(path)
    File.split(path)[0]
  end
  
  html_srcs = FileList['templates/**/*.*']
  html_triples = html_srcs.map {|f| [f, "src/#{f}", get_dir("src/#{f}")]}

  html_triples.each do |src, tgt, dir|
    directory dir
    file tgt => [src, dir] do
      sh "java -jar scripts/htmlcompressor-1.1.jar #{src} -o #{tgt}"
    end
  end

  desc 'Compress html using htmlcompressor, save compressed to src/templates'
  task :compress => html_srcs.map {|f| "src/#{f}"}
end

namespace :watch do
  desc 'Run rake css:compile when modification detected '
  task :css => ["css:compile"] do
    sh 'while inotifywait -e modify less/; do rake css; done'
  end

  desc 'Run rake html:compress when modification detected'
  task :html => ["html:compress"] do
    sh 'while inotifywait -r -e modify templates/; do rake html:compress; done'
  end
  
  desc 'Watch css, html; after Ctrl+C, process sh should be manually killed'
  task :all => ["css:compile", "html:compress"] do
    t1 = Thread.new do
      sh 'while inotifywait -e modify less/; do
rake css:compile; done'
    end
    t2 = Thread.new do
      sh 'while inotifywait -r -e modify templates/; do
rake html:compress; done'
    end
    t1.join
    t2.join
  end
end
