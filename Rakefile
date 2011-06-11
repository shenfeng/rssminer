require 'rake/clean'
require 'tempfile'
require 'rubygems'
require 'closure-compiler'
task :default => :test

def get_file_as_string(filename)
  data = ''
  f = File.open(filename, "r")
  f.each_line do |line|
    data += line
  end
  return data
end

def compress(type, source, target)
  sh "java -jar 'scripts/yuicompressor-2.4.6.jar' --type #{type} " +
    "--charset utf-8 -o #{target} \"#{source}\" 2> /dev/null > /dev/null"
end

desc "Download jstestdriver from web"
task :download do
  unless File.exists? 'scripts/JsTestDriver-1.3.2.jar'
    sh 'wget -O scripts/JsTestDriver-1.3.2.jar http://js-test-driver.googlecode.com/files/JsTestDriver-1.3.2.jar'
  end
  unless File.exists? 'scripts/htmlcompressor-1.3.1.jar'
    sh 'wget -O scripts/htmlcompressor-1.3.1.jar http://htmlcompressor.googlecode.com/files/htmlcompressor-1.3.1.jar'
  end
end

desc "Prepare for test"
task :prepare => ["css:compile", "js:tmpls"]

desc "Prepare fro production"
task :prepare_prod => ["css:compress", "js:minify"]

desc "Run development server"
task :run => ["prepare"] do
  sh 'scripts/run --profile development'
end

desc "Run production server"
task :run_prod => ["prepare_prod"] do
  sh 'scripts/run --profile production'
end

namespace :js do
  CLEAN.include('public/js/freader/tmpls.js','public/js/freader.min.js')
  desc 'start jstestdriver server'
  task :startserver do
    sh 'java -jar scripts/JsTestDriver-1.3.2.jar --port 9876 --browser `which firefox`'
  end

  desc 'Run js unit test against running jstestdriver server'
  task :unit => :download do
    sh 'java -jar scripts/JsTestDriver-1.3.2.jar --tests all --captureConsole'
  end

  desc 'Combine all js into one, minify it using google closure'
  task :minify => :tmpls do
    print "Running closure against all js file, please wait....\n"
    files = FileList['public/js/jquery-1.6.1.js',
                     'public/js/underscore.js',
                     'public/js/backbone.js',
                     'public/js/handlebars-1.0.0.beta.2.js',
                     'public/js/freader/tmpls.js',
                     'public/js/freader/application.js']
    src = '';
    # closure = Closure::Compiler.new(:compilation_level =>
    #                                 'ADVANCED_OPTIMIZATIONS')
    closure = Closure::Compiler.new()

    files.each do |f|
      src += get_file_as_string(f);
    end
    minified = closure.compile(src);
    File.open("public/js/freader.min.js", 'w') {|f| f.write(minified)}
    # minified = closure.compile(src);
    # File.open("/tmp/freader.js", 'w') {|f| f.write(src)}

  end

  desc "Generate tmpls.js"
  task :tmpls => ["html:compress"] do
    print "Generate tmpls.js, please wait....\n"
    html_tmpls = FileList['src/templates/js-tmpls/**/*.*']
    data = "(function(){var tmpls = {};"
    html_tmpls.each do |f|
      text = get_file_as_string(f).gsub(/\s+/," ")
      name = File.basename(f,".tpl")
      data += "tmpls." + name + " = Handlebars.compile('" + text + "');\n"
    end
    data += "window.Freader = $.extend(window.Freader, {tmpls: tmpls})})();\n"
    File.open("public/js/freader/tmpls.js", 'w') {|f| f.write(data)}
  end
end

namespace :css do
  CLEAN.include('public/css/*')
  desc 'Compile scss, Generate css'
  task :compile do
    scss = FileList['scss/**/*.scss'].exclude('scss/**/_*.scss')
    scss.each do |source|
      target = source.sub(/scss$/, 'css').sub(/^scss/, 'public/css')
      sh "sass -t expanded -g --cache-location /tmp #{source} #{target}"
    end
  end
  desc 'Compile scss, Compress generated css'
  task :compress do
    scss = FileList['scss/**/*.scss'].exclude('scss/**/_*.scss')
    scss.each do |source|
      target = source.sub(/scss$/, 'css').sub(/^scss/, 'public/css')
      sh "sass -t compressed --cache-location /tmp #{source} #{target}"
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
      # sh "cp #{src} #{tgt}"
      sh "java -jar scripts/htmlcompressor-1.3.1.jar --charset utf8 #{src} -o #{tgt}"
    end
  end

  desc 'Compress html using htmlcompressor, save compressed to src/templates'
  task :compress => html_srcs.map {|f| "src/#{f}"}
end

namespace :watch do
  desc 'Watch css, html; after Ctrl+C, process sh should be manually killed'
  task :all => ["css:compile", "js:tmpls", "download"] do
    t1 = Thread.new do
      sh 'while inotifywait -e modify scss/; do rake css:compile; done'
    end
    t2 = Thread.new do
      sh 'while inotifywait -r -e modify templates/; do rake js:tmpls; done'
    end
    t1.join
    t2.join
  end
end
