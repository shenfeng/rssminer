task :default => :test

def get_file_as_string(filename)
  data = ''
  f = File.open(filename, "r")
  f.each_line do |line|
    data += line
  end
  return data
end

def get_dir(path)
  File.split(path)[0]
end

version = Time.now.strftime("%Y%m%d%H%M") # timestamp

file 'bin/closure-compiler.jar' do
  mkdir_p 'bin'
  rm_rf '/tmp/closure-compiler.zip'
  sh 'wget http://closure-compiler.googlecode.com/files/compiler-latest.zip' +
    ' -O /tmp/closure-compiler.zip'
  rm_rf '/tmp/compiler.jar'
  sh 'unzip /tmp/closure-compiler.zip compiler.jar -d /tmp'
  rm_rf '/tmp/closure-compiler.zip'
  mv '/tmp/compiler.jar', 'bin/closure-compiler.jar'
end

file "bin/htmlcompressor.jar" do
  mkdir_p 'bin'
  sh 'wget http://htmlcompressor.googlecode.com/files/htmlcompressor-1.3.1.jar' +
    ' -O bin/htmlcompressor.jar'
end

task :deps => ['bin/closure-compiler.jar', "bin/htmlcompressor.jar"]

freader_jss = FileList['public/js/lib/jquery.js',
                       'public/js/lib/jquery-ui-1.8.13.custom.js',
                       'public/js/lib/underscore.js',
                       'public/js/lib/backbone.js',
                       'public/js/lib/mustache.js',
                       'public/js/freader/tmpls.js',
                       'public/js/freader/util.js',
                       'public/js/freader/models.js',
                       'public/js/freader/views.js',
                       'public/js/freader/magic.js',
                       'public/js/freader/application.js']

desc "Clean generated files"
task :clean  do
  rm_rf 'public/js/freader/tmpls.js'
  rm_rf 'src/templates'
  sh 'rm public/js/freader*min.js || exit 0'
  sh 'rm public/css/*.css || exit 0'
end

desc "Prepare for development"
task :prepare => ["css:compile",:html_compress, "js:tmpls"]

desc "Prepare for production"
task :prepare_prod => ["css:compress", "js:minify"]

desc "Run server in dev profile"
task :run => :prepare do
  sh 'scripts/run --profile dev'
end

desc "Run server in production profile"
task :run_prod => :prepare_prod do
  sh 'scripts/run --profile prod'
end

desc 'Deploy to production'
task :deploy => [:clean, :test, :prepare_prod] do
  sh "scripts/deploy"
end

desc "Run unit test"
task :test => :prepare do
  sh 'lein test'
end

namespace :js do
  desc "Generate template js resouces"
  task :tmpls => :html_compress do
    print "Generating tmpls.js, please wait....\n"
    html_tmpls = FileList['src/templates/js-tmpls/**/*.*']
    data = "(function(){var tmpls = {};"
    html_tmpls.each do |f|
      text = get_file_as_string(f).gsub(/\s+/," ")
      name = File.basename(f,".tpl")
      data += "tmpls." + name + " = '" + text + "';\n"
    end
    data += "window.Freader = window.$.extend(window.Freader, {tmpls: tmpls})})();\n"
    File.open("public/js/freader/tmpls.js", 'w') {|f| f.write(data)}
  end

  desc 'Combine all js into one, minify it using google closure'
  task :minify => [:tmpls, :deps] do
    target = "public/js/freader-#{version}-min.js"

    source_arg = ''
    freader_jss.each do |js|
      source_arg += " --js #{js} "
    end

    sh 'java -jar bin/closure-compiler.jar --warning_level QUIET' +
      " --js_output_file '#{target}' #{source_arg}"
  end
end

namespace :css do
  scss = FileList['scss/**/*.scss'].exclude('scss/**/_*.scss')
  desc 'Compile scss, generate css'
  task :compile do
    scss.each do |source|
      target = source.sub(/scss$/, 'css').sub(/^scss/, 'public/css')
      sh "sass -t expanded -g --cache-location /tmp #{source} #{target}"
    end
  end
  desc 'Compile scss, compress generated css'
  task :compress do
    versioned = "-#{version}-min.css"
    scss.each do |source|
      target = source.sub(/\.scss$/, versioned).sub(/^scss/, 'public/css')
      sh "sass -t compressed --cache-location /tmp #{source} #{target}"
      sh "sed -i \"s/{VERSION}/#{version}/g\" #{target}"
    end
  end
end

html_srcs = FileList['templates/**/*.*']
html_triples = html_srcs.map {|f| [f, "src/#{f}", get_dir("src/#{f}")]}

html_triples.each do |src, tgt, dir|
  directory dir
  file tgt => [src, dir] do
    sh "java -jar bin/htmlcompressor.jar --charset utf8 #{src} -o #{tgt}"
    sh "sed -i \"s/{VERSION}/#{version}/g\" #{tgt}"
  end
end

desc 'Compress html using htmlcompressor, save compressed to src/templates'
task :html_compress => html_srcs.map {|f| "src/#{f}"}

namespace :watch do
  desc 'Watch css, html'
  task :all => [:deps, "css:compile", "js:tmpls"] do
    t1 = Thread.new do
      sh 'while inotifywait -e modify scss/; do rake css:compile; done'
    end
    t2 = Thread.new do
      sh 'while inotifywait -r -e modify templates/; do rake js:tmpls; done'
    end
    trap(:INT) {
      sh "killall inotifywait"
    }
    t1.join
    t2.join
  end
end
