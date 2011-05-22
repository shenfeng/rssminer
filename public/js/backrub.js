(function() {
  var Backrub, proto, _i, _len, _ref;
  Backrub = {
    _Genuine: {
      nameLookup: Handlebars.JavaScriptCompiler.prototype.nameLookup,
      mustache: Handlebars.Compiler.prototype.mustache
    },
    _getPath: function(path, base, wrap) {
      var parts, prev;
      wrap = wrap || false;
      base = base || window;
      prev = base;
      if (path === null || path === void 0) {
        throw new Error("Path is undefined or null");
      }
      parts = path.split(".");
      _.each(parts, function(p) {
        prev = base;
        base = p === "" ? base : base[p];
        if (!base) {
          throw new Error("cannot find given path '" + path + "'");
          return {};
        }
      });
      if (typeof base === "function" && wrap) {
        return _.bind(base, prev);
      } else {
        return base;
      }
    },
    _resolveValue: function(attr, model) {
      var model_info, value;
      model_info = Backrub._resolveIsModel(attr, model);
      if (model_info.is_model) {
        return model_info.model.get(model_info.attr);
      } else if (model_info.is_model === null) {
        return attr;
      } else {
        value = (function() {
          try {
            return Backrub._getPath(model_info.attr, model_info.model, true);
          } catch (error) {

          }
        })();
        if (typeof value === "function") {
          return value();
        } else {
          return value;
        }
      }
    },
    _resolveIsModel: function(attr, model) {
      var is_model;
      is_model = false;
      attr = attr && ((typeof attr.charAt == "function" ? attr.charAt(0) : void 0) === "@") ? (is_model = true, model = model.model, attr.substring(1)) : attr && model.model && model.model.get && model.model.get(attr) !== void 0 ? (is_model = true, model = model.model, attr) : model[attr] !== void 0 ? attr : (model = null, is_model = null, attr);
      return {
        is_model: is_model,
        attr: attr,
        model: model,
        bind: function(callback) {
          if (model && model.bind) {
            return model.bind("change:" + attr, callback);
          }
        }
      };
    },
    _bindIf: function(attr, context) {
      var model_info, view;
      if (context) {
        view = Backrub._createBindView(attr, this, context);
        model_info = Backrub._resolveIsModel(attr, this);
        model_info.bind(function() {
          if (context.data.exec.isAlive()) {
            view.rerender();
            return context.data.exec.makeAlive();
          }
        });
        view.render = function() {
          var fn;
          fn = Backrub._resolveValue(this.attr, this.model) ? context.fn : context.inverse;
          return new Handlebars.SafeString(this.span(fn(this.model, {
            data: context.data
          })));
        };
        return view.render();
      } else {
        throw new Error("No block is provided!");
      }
    },
    _bindAttr: function(attrs, context, model) {
      var id, outAttrs, self;
      id = _.uniqueId('ba');
      outAttrs = [];
      self = model || this;
      _.each(attrs, function(attr, k) {
        var model_info, value;
        model_info = Backrub._resolveIsModel(attr, self);
        value = Backrub._resolveValue(attr, self);
        outAttrs.push("" + k + "=\"" + value + "\"");
        return model_info.bind(function() {
          var el;
          if (context.data.exec.isAlive()) {
            el = $("[data-baid='" + id + "']");
            if (el.length === 0) {
              return model_info.model.unbind("change" + model_info.attr);
            } else {
              return el.attr(k, Backrub._resolveValue(attr, self));
            }
          }
        });
      });
      if (outAttrs.length > 0) {
        outAttrs.push("data-baid=\"" + id + "\"");
      }
      return new Handlebars.SafeString(outAttrs.join(" "));
    },
    _createView: function(viewProto, options) {
      var v;
      v = new viewProto(options);
      if (!v) {
        throw new Error("Cannot instantiate view");
      }
      v._ensureElement = Backrub._BindView.prototype._ensureElement;
      v.span = Backrub._BindView.prototype.span;
      v.live = Backrub._BindView.prototype.live;
      v.textAttributes = Backrub._BindView.prototype.textAttributes;
      v.bvid = "" + (_.uniqueId('bv'));
      return v;
    },
    _createBindView: function(attr, model, context) {
      var view;
      view = new Backrub._BindView({
        attr: attr,
        model: model,
        context: context,
        prevThis: model
      });
      context.data.exec.addView(view);
      if (context.hash) {
        view.tagName = context.hash.tag || view.tagName;
        delete context.hash.tag;
        view.attributes = context.hash;
      }
      return view;
    },
    _BindView: Backbone.View.extend({
      tagName: "span",
      _ensureElement: function() {
        return null;
      },
      live: function() {
        return $("[data-bvid='" + this.bvid + "']");
      },
      initialize: function() {
        _.bindAll(this, "render", "rerender", "span", "live", "value", "textAttributes");
        this.bvid = "" + (_.uniqueId('bv'));
        this.attr = this.options.attr;
        this.prevThis = this.options.prevThis;
        return this.hbContext = this.options.context;
      },
      value: function() {
        return Backrub._resolveValue(this.attr, this.model);
      },
      textAttributes: function() {
        this.attributes = this.attributes || this.options.attributes || {};
        if (!this.attributes.id && this.id) {
          this.attributes.id = this.id;
        }
        if (!this.attributes["class"] && this.className) {
          this.attributes["class"] = this.className;
        }
        return Backrub._bindAttr(this.attributes, this.hbContext, this.prevThis || this).string;
      },
      span: function(inner) {
        return "<" + this.tagName + " " + (this.textAttributes()) + " data-bvid=\"" + this.bvid + "\">" + inner + "</" + this.tagName + ">";
      },
      rerender: function() {
        return this.live().replaceWith(this.render().string);
      },
      render: function() {
        return new Handlebars.SafeString(this.span(this.value()));
      }
    })
  };
  Handlebars.Compiler.prototype.mustache = function(mustache) {
    var id;
    if (mustache.params.length || mustache.hash) {
      return Backrub._Genuine.mustache.call(this, mustache);
    } else {
      id = new Handlebars.AST.IdNode(['bind']);
      mustache = new Handlebars.AST.MustacheNode([id].concat([mustache.id]), mustache.hash, !mustache.escaped);
      return Backrub._Genuine.mustache.call(this, mustache);
    }
  };
  Handlebars.JavaScriptCompiler.prototype.nameLookup = function(parent, name, type) {
    if (type === 'context') {
      return "\"" + name + "\"";
    } else {
      return Backrub._Genuine.nameLookup.call(this, parent, name, type);
    }
  };
  Backbone.dependencies = function(onHash, base) {
    var event, path, setupEvent, _results;
    base = base || this;
    if (!base.trigger && !base.bind) {
      throw new Error("Not a Backbone.Event object");
    }
    setupEvent = function(event, path) {
      var attr, e, object, parts, _i, _len, _ref, _results;
      parts = event.split(" ");
      attr = parts[0];
      object = Backrub._getPath(path, base);
      _ref = parts.slice(1);
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        e = _ref[_i];
        _results.push(object != null ? object.bind(e, function() {
          return base.trigger("change:" + attr);
        }) : void 0);
      }
      return _results;
    };
    _results = [];
    for (event in onHash) {
      path = onHash[event];
      _results.push(setupEvent(event, path));
    }
    return _results;
  };
  _ref = [Backbone.Model.prototype, Backbone.Controller.prototype, Backbone.Collection.prototype, Backbone.View.prototype];
  for (_i = 0, _len = _ref.length; _i < _len; _i++) {
    proto = _ref[_i];
    _.extend(proto, {
      dependencies: Backbone.dependencies
    });
  }
  Backbone.Backrub = function(template) {
    _.bindAll(this, "addView", "render", "makeAlive", "isAlive");
    this.compiled = Handlebars.compile(template, {
      data: true,
      stringParams: true
    });
    this._createdViews = {};
    this._aliveViews = {};
    this._alive = false;
    return this;
  };
  _.extend(Backbone.Backrub.prototype, {
    render: function(options) {
      var self;
      self = this;
      return this.compiled(options, {
        data: {
          exec: this
        }
      });
    },
    makeAlive: function(base) {
      var currentViews, query, self;
      base = base || $("body");
      query = [];
      currentViews = this._createdViews;
      this._createdViews = {};
      _.each(currentViews, function(view, bvid) {
        return query.push("[data-bvid='" + bvid + "']");
      });
      this._alive = true;
      self = this;
      $(query.join(","), base).each(function() {
        var el, view, _ref;
        el = $(this);
        view = currentViews[el.attr("data-bvid")];
        view.el = el;
        view.delegateEvents();
        return (_ref = view.alive) != null ? _ref.call(view) : void 0;
      });
      return _.extend(this._aliveViews, currentViews);
    },
    isAlive: function() {
      return this._alive;
    },
    addView: function(view) {
      return this._createdViews[view.bvid] = view;
    },
    removeView: function(view) {
      delete this._createdViews[view.bvid];
      delete this._aliveViews[view.bvid];
      return delete view;
    }
  });
  Backbone.TemplateView = Backbone.View.extend({
    initialize: function(options) {
      this.template = this.template || options.template;
      if (!this.template) {
        throw new Error("Template is missing");
      }
      return this.compile = new Backbone.Backrub(this.template);
    },
    render: function() {
      try {
        $(this.el).html(this.compile.render(this));
        this.compile.makeAlive(this.el);
      } catch (e) {
        console.error(e.stack);
      }
      return this.el;
    }
  });
  Handlebars.registerHelper("view", function(viewName, context) {
    var execContext, key, resolvedOptions, v, val, view, _ref;
    execContext = context.data.exec;
    view = Backrub._getPath(viewName);
    resolvedOptions = {};
    _ref = context.hash;
    for (key in _ref) {
      val = _ref[key];
      resolvedOptions[key] = Backrub._resolveValue(val, this) || val;
    }
    v = Backrub._createView(view, resolvedOptions);
    execContext.addView(v);
    v.render = function() {
      return new Handlebars.SafeString(this.span(context(this, {
        data: context.data
      })));
    };
    return v.render(v);
  });
  Handlebars.registerHelper("bind", function(attrName, context) {
    var execContext, model_info, view;
    execContext = context.data.exec;
    view = Backrub._createBindView(attrName, this, context);
    model_info = Backrub._resolveIsModel(attrName, this);
    model_info.bind(function() {
      if (execContext.isAlive()) {
        view.rerender();
        return execContext.makeAlive();
      }
    });
    return new Handlebars.SafeString(view.render());
  });
  Handlebars.registerHelper("bindAttr", function(context) {
    return _.bind(Backrub._bindAttr, this)(context.hash, context);
  });
  Handlebars.registerHelper("if", function(attr, context) {
    return _.bind(Backrub._bindIf, this)(attr, context);
  });
  Handlebars.registerHelper("unless", function(attr, context) {
    var fn, inverse;
    fn = context.fn;
    inverse = context.inverse;
    context.fn = inverse;
    context.inverse = fn;
    return _.bind(Backrub._bindIf, this)(attr, context);
  });
  Handlebars.registerHelper("collection", function(attr, context) {
    var colAtts, colTagName, colView, colViewPath, collection, execContext, itemAtts, itemTagName, itemView, itemViewPath, item_view, options, setup, view, views;
    execContext = context.data.exec;
    collection = Backrub._resolveValue(attr, this);
    if (!(collection.each != null)) {
      throw new Error("not a backbone collection!");
    }
    options = context.hash;
    colViewPath = options != null ? options.colView : void 0;
    if (colViewPath) {
      colView = Backrub._getPath(colViewPath);
    }
    colTagName = (options != null ? options.colTag : void 0) || "ul";
    itemViewPath = options != null ? options.itemView : void 0;
    if (itemViewPath) {
      itemView = Backrub._getPath(itemViewPath);
    }
    itemTagName = (options != null ? options.itemTag : void 0) || "li";
    colAtts = {};
    itemAtts = {};
    _.each(options, function(v, k) {
      if (k.indexOf("Tag") > 0 || k.indexOf("View") > 0) {
        return;
      }
      if (k.indexOf("col") === 0) {
        return colAtts[k.substring(3).toLowerCase()] = v;
      } else if (k.indexOf("item") === 0) {
        return itemAtts[k.substring(4).toLowerCase()] = v;
      }
    });
    view = colView ? Backrub._createView(colView, {
      model: collection,
      attributes: colAtts,
      context: context,
      tagName: (options != null ? options.colTag : void 0) ? colTagName : colView.prototype.tagName
    }) : new Backrub._BindView({
      tagName: colTagName,
      attributes: colAtts,
      attr: attr,
      model: this,
      context: context
    });
    execContext.addView(view);
    views = {};
    item_view = function(m) {
      var mview;
      mview = itemView ? Backrub._createView(itemView, {
        model: m,
        attributes: itemAtts,
        context: context,
        tagName: (options != null ? options.itemTag : void 0) ? itemTagName : itemView.prototype.tagName
      }) : new Backrub._BindView({
        tagName: itemTagName,
        attributes: itemAtts,
        model: m,
        context: context
      });
      execContext.addView(mview);
      mview.render = function() {
        return this.span(context(this, {
          data: context.data
        }));
      };
      return mview;
    };
    setup = function(col, mainView, childViews) {
      col.each(function(m) {
        var mview;
        mview = item_view(m);
        return childViews[m.cid] = mview;
      });
      return mainView.render = function() {
        var rendered;
        rendered = _.map(childViews, function(v) {
          return v.render();
        });
        return new Handlebars.SafeString(this.span(rendered.join("\n")));
      };
    };
    setup(collection, view, views);
    collection.bind("refresh", function() {
      if (execContext.isAlive()) {
        views = {};
        setup(collection, view, views);
        view.rerender();
        return execContext.makeAlive();
      }
    });
    collection.bind("add", function(m) {
      var mview;
      if (execContext.isAlive()) {
        mview = item_view(m);
        views[m.cid] = mview;
        view.live().append(mview.render());
        return execContext.makeAlive();
      }
    });
    collection.bind("remove", function(m) {
      var mview;
      if (execContext.isAlive()) {
        mview = views[m.cid];
        mview.live().remove();
        return execContext.removeView(mview);
      }
    });
    return view.render();
  });
}).call(this);
