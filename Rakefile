task :default => :all_test

version = Time.now.strftime("%Y%m%d%H%M") # timestamp
jscompiler = "closure-compiler.jar"
htmlcompressor = "htmlcompressor.jar"
luke = "lukeall-3.5.0.jar"

def get_file_as_string(filename)
  return open(filename, 'r'){|f| f.read }
end

def get_dir(path)
  File.split(path)[0]
end

def gen_jstempls(folder)
  print "Generating #{folder}-tmpls.js, please wait....\n"
  html_tmpls = FileList["src/templates/tmpls/#{folder}/**/*.*"]
  data = "(function(){var tmpls = {};"
  html_tmpls.each do |f|
    text = get_file_as_string(f).gsub(/\s+/," ")
    name = File.basename(f, ".tpl")
    data += "tmpls." + name + " = '" + text + "';\n"
  end
  data += "window.RM = {tmpls: tmpls};})();\n"
  File.open("public/js/gen/#{folder}-tmpls.js", 'w') {|f| f.write(data)}
end

def minify_js(target, jss)
  jscompiler = "closure-compiler.jar"
  source_arg = ''
  jss.each do |js|
    source_arg += " --js #{js} "
  end
  # ADVANCED_OPTIMIZATIONS SIMPLE_OPTIMIZATIONS
  sh "java -jar thirdparty/#{jscompiler} --warning_level QUIET " +
    "--compilation_level SIMPLE_OPTIMIZATIONS " +
    "--js_output_file '#{target}' #{source_arg}"
end

file "thirdparty/#{jscompiler}" do
  mkdir_p "thirdparty"
  sh 'wget http://closure-compiler.googlecode.com/files/compiler-latest.zip' +
    ' -O /tmp/closure-compiler.zip'
  rm_rf '/tmp/compiler.jar'
  sh 'unzip /tmp/closure-compiler.zip compiler.jar -d /tmp'
  rm_rf '/tmp/closure-compiler.zip'
  mv '/tmp/compiler.jar', "thirdparty/#{jscompiler}"
end

file "thirdparty/#{htmlcompressor}" do
  mkdir_p "thirdparty"
  sh 'wget http://htmlcompressor.googlecode.com/files/htmlcompressor-1.5.3.jar' +
    " -O thirdparty/#{htmlcompressor}"
end

file "thirdparty/#{luke}" do
  mkdir_p "thirdparty"
  sh "wget http://luke.googlecode.com/files/#{luke} " +
    "-O thirdparty/#{luke}"
end

task :deps => ["thirdparty/#{jscompiler}",
               "thirdparty/#{luke}",
               "thirdparty/#{htmlcompressor}"]

landing_jss = FileList['public/js/lib/slides.min.jquery.js',
                       'public/js/rssminer/tooltip.js',
                       'public/js/rssminer/landing.js']

app_jss = FileList['public/js/lib/jquery-ui-1.8.18.custom.js',
                   'public/js/lib/underscore.js',
                   'public/js/lib/mustache.js',
                   'public/js/rssminer/i18n.js',
                   'public/js/gen/app-tmpls.js',
                   'public/js/rssminer/util.js',
                   'public/js/rssminer/placeholder.js',
                   'public/js/rssminer/ajax.js',
                   'public/js/rssminer/router.js',
                   'public/js/rssminer/layout.js',
                   'public/js/rssminer/rm_data.js',
                   'public/js/rssminer/search.js',
                   'public/js/rssminer/ct_menu.js',
                   'public/js/rssminer/keyboard.js',
                   'public/js/rssminer/tooltip.js',
                   'public/js/rssminer/app.js']

desc "Clean generated files"
task :clean  do
  rm_rf 'public/js/rssminer/tmpls.js'
  rm_rf 'src/templates'
  rm_rf 'public/rssminer.crx'
  rm_rf 'public/js/gen'
  rm_rf "public/css"
  rm_rf "classes"
  sh 'rm -vf public/js/*min.js'
  # sh 'find . -name "*.class" | xargs rm'
end

desc "Prepare for development"
task :prepare => [:css_compile,:html_compress, "js:tmpls"]

desc "Prepare for production"
task :prepare_prod => [:css_compile, "js:minify"]

desc "lein swank"
task :swank => [:javac, :prepare] do
  sh "lein swank"
end

desc 'Deploy to production'
task :deploy => [:clean, :chrome, :test, :prepare_prod] do
  sh "scripts/deploy"
end

desc "Javac"
task :javac do
  sh "scripts/javac"
end

desc "Javac debug"
task :javac_debug do
  sh "scripts/javac with-test"
end

desc "Run junit test"
task :junit => [:javac_debug] do
  sh './scripts/junit_test'
end

desc "Run all test"
task :all_test => [:test, :junit]

desc "Run lein unit test"
task :test => [:prepare, :java] do
  sh 'lein test'
end

desc "Generate TAGS using etags for clj"
task :etags do
  rm_rf 'TAGS'
  sh %q"find . \! -name '.*' -name '*.clj' | xargs etags --regex='/[ \t\(]*def[a-z]* \([a-z-!0-9?]+\)/\1/'"
end

namespace :js do
  desc "Generate template js resouces"
  task :tmpls => :html_compress do
    mkdir_p "public/js/gen"
    gen_jstempls("app");
    gen_jstempls("chrome");
    # sh 'mv public/js/gen/chrome-tmpls.js chrome/tmpls.js'
  end

  desc 'Combine all js into one, minify it using google closure'
  task :minify => [:tmpls, :deps] do
    minify_js("public/js/app-min.js", app_jss);
    minify_js("public/js/landing-min.js", landing_jss);
  end
end

scss = FileList['scss/**/*.scss'].exclude('scss/**/_*.scss')
desc 'Compile scss, compress generated css'
task :css_compile do
  mkdir_p "public/css"
  scss.each do |source|
    target = source.sub(/scss$/, 'css').sub(/^scss/, 'public/css')
    sh "sass -t compressed --cache-location /tmp #{source} #{target}"
  end
  sh "find public/css/ -type f " +
    "| xargs -I {} sed -i -e \"s/{VERSION}/#{version}/g\" {}"
  # os x sed will generate many file end with -e
  sh "find public/css -type f -name \"*-e\" | xargs rm -f"
  sh 'mv public/css/chrome.css chrome/style.css'
end

desc "create chrome extension"
task :chrome do
  # sh 'google-chrome --pack-extension=chrome ' + '--pack-extension-key=conf/chrome.pem'
  # sh 'mv chrome.crx public/rssminer.crx'
end

desc "update dev mysql config file, restart mysql"
task :mysql_dev do
  sh 'sudo /etc/init.d/mysql stop'
  sh 'sudo rm /tmp/mysql -rf && sudo rm /tmp/mysql.log -f'
  sh 'mkdir /tmp/mysql'
  sh 'sudo cp conf/my-dev.cnf /etc/mysql/my.cnf && sudo mysql_install_db'
  # sh 'sudo chown mysql:mysql /tmp/mysql -R'
  sh 'sudo /etc/init.d/mysql start'
  sh './scripts/admin restore-db'
end

desc "update dev mysql config file, restart mysql"
task :mysql_prod do
  sh 'sudo /etc/init.d/mysql stop'
  sh 'sudo cp conf/my.cnf /etc/mysql/my.cnf'
  sh 'sudo /etc/init.d/mysql start'
  # sh 'rake db:restore_db'
end

desc 'Compress html using htmlcompressor, save compressed to src/templates'
task :html_compress => :deps do
  sh "scripts/compress_html"
end

desc "Using luke to inspect luence index"
task :luke do
  sh "java -jar thirdparty/#{luke} -index /var/rssminer/index &"
end

desc "Rebuild index"
task :rebuild_index => :javac do
  sh './scripts/admin rebuild-index'
end

namespace :db do
  desc "Reload database with production data"
  task :backup_prod => :javac do
    sh './scripts/admin backup-db && ./scripts/admin restore-db && ./scripts/admin rebuild-index'
  end

  desc "Restore db from latest backup"
  task :restore_db => :javac do
    sh './scripts/admin restore-db && ./scripts/admin rebuild-index'
  end
end

namespace :run do
  desc "Run server in dev profile"
  task :dev => [:prepare, :javac] do
    sh 'scripts/run --profile dev'
  end

  desc "Compile and run"
  task :aot do
    sh 'scripts/aot_run'
  end

  desc "Run server in production profile"
  task :prod => [:prepare_prod, :javac] do
    sh 'scripts/run --profile prod --static-server //s.rss-miner.com'
  end

  desc "Restore db from latest backup, Run server in dev profile"
  task :restore_db_dev => ["db:restore_db", "run:dev"]
end

def get_mtime(patten)
  mtime_total = 0
  FileList[patten].each do |f|
    mtime_total += File.mtime(f).to_i
  end
  return mtime_total
end

def watch_change(patten, cb)
  t = Thread.new do
    mtime_total = get_mtime(patten)
    while true do
      sleep 0.1                #  sleep 100ms
      n = get_mtime(patten)
      # puts n
      if n != mtime_total
        cb.call()
        mtime_total = n
      end
    end
  end
  return t
end

def has_inotify()
  begin
    sh "which inotifywait"
    return true
  rescue
    return false
  end
end

desc "reload browser"
task :reload do
  sh 'wget http://localhost:9090/dev/c -q -O /dev/null || exit 0'
end

desc 'Watch css, html'
task :watch => [:deps, :css_compile, "js:tmpls"] do
  if has_inotify
    t1 = Thread.new {
      sh 'while inotifywait -r -e modify scss/; do rake css_compile reload; done'
    }
    t2 = Thread.new {
      sh 'while inotifywait -r -e modify templates/; do rake js:tmpls reload; done'
    }
    t3 = Thread.new {
      sh 'while inotifywait -r -e modify public/js/rssminer; do rake reload; done'
    }
  elsif
    t1 = watch_change('scss/**/*.*', lambda {sh 'rake css_compile reload'})
    t2 = watch_change('templates/**/*.*', lambda {sh 'rake js:tmpls reload'})
    t2 = watch_change('public/js/rssminer/**/*.*', lambda {sh 'rake reload'})
  end
  trap(:INT) {
    sh "killall inotifywait || exit 0"
    exit
  }
  t1.join
  t2.join
  t3.join
end
