/* Extend Backbone's Model and Collection and Sync to support
 * 1. nested data
 * 2. only send diff to server, diff computed in a recursive manner
 *    a) diff begin with call to snapshot()
 *    b)  server respond with a diff
 *    c) example data format
 *        request or responce : {
 *          _op: '!',
 *          _data: {
 *             name: 'name',
 *             nested_data: [
 *                 {_op: '+', _data: {.....}}
 *             ]
 *          }
 *        }
 *    d) three _op are define !(update), +(create), -(delete)
 */

(function() {
  var root = window;            // save a reference
  var Backbone = root.Backbone, // js2-mode complains if don't
      $ = root.jQuery,
      notif = root.Rssminer.notif,
      JSON = root.JSON,
      loading = 'Loading...',
      _ = root._;

  var backboneSync = Backbone.sync; // save it;

  function handler(ajax) { // hide notification when finished ajax request
    return ajax.success(function () {
      notif.hide(loading);
    }).error(function (xhr, status, code) {
      notif.error($.parseJSON(xhr.responseText).message);
    });
  };

  var Model = Backbone.Model.extend({

    toJSON : function() {         // recursive
      var val, json = _.clone(this.attributes);
      for(var attr in json) {
        val = json[attr];
        if(val && json.hasOwnProperty(attr) &&  _.isFunction(val.toJSON)) {
          json[attr] = val.toJSON();
        }
      }
      return json;
    },

    // copy and modify Backbone's set to support recursive
    set : function(attrs, options) {
      // Extract attributes and options.
      options = options || {};
      if (!attrs) return this;
      if (attrs.attributes) attrs = attrs.attributes;

      // Run validation.
      if (!options.silent && this.validate &&
          !this._performValidation(attrs, options)) {
        return false;
      }
      // Check for changes of `id`.
      if (this.idAttribute in attrs) this.id = attrs[this.idAttribute];

      // We're about to start triggering change events.
      var alreadyChanging = this._changing;
      this._changing = true;

      // Update attributes.
      for (var attr in attrs) {
        this._set(attr, attrs[attr], options);
      }

      // Fire the `"change"` event, if the model has been changed.
      if (!alreadyChanging && !options.silent && this._changed) {
        this.change(options);
      }
      this._changing = false;
      return this;
    },

    _set : function(attr, newval, options) {
      options = options || {};
      var Klass =  this[attr],
          that = this,
          now = this.attributes,
          oldval = now[attr],
          escaped = this._escapedAttributes;

      if (_.isEqual(newval, oldval)) return;

      // Use duck typing to check if Klass inherits Collection
      if(_.isArray(newval) && Klass && _.isFunction(Klass.prototype.reset)) {
        if(oldval) {
          now[attr] = oldval.reset(newval, options);
        } else {
          now[attr] = new Klass(newval, options);
          // TODO change event maybe trigger more than once
          var delegateEvent = function () {
            that.trigger("change:" + attr, that, now[attr], options);
            that.trigger("change", that, now[attr], options);
          };
          now[attr].bind('add', delegateEvent)
            .bind('remove', delegateEvent)
            .bind('reset', delegateEvent);
        }
        now[attr].parent = this; // save a parent reference;
      } else if ($.isPlainObject(newval) && Klass // Model
                 && _.isFunction(Klass.prototype.set)) {
        if(oldval) {
          oldval.set(newval, options);
        } else {
          now[attr] = new Klass(newval, options);
          now[attr].bind('change', function () {
            that.trigger("change:" + attr, that, now[attr], options);
          });
        }
        now[attr].parent = this; // save a parent reference;
      } else {
        now[attr] = newval;
      }

      delete escaped[attr];
      this._changed = true;
      if (!options.silent) {
        this.trigger('change:' + attr, this, now[attr], options);
      }
    },

    //  save diff since last time call snapshot
    savediff : function (options) {
      options = options || {};
      var sync = (this.sync || Backbone.sync),
          that = this;
      return sync.call(this,'sync', this, options);
    },

    // take a snapshot the model's state, later used to compute diff
    snapshot : function() {
      // shadowed, nested are not copyed
      this._snapshotAttributes = _.clone(this.attributes);
      _.each(this.attributes, function(val, key) {
        // model or collection, take their own
        if(val && _.isFunction(val.snapshot)){
          val.snapshot();
        }
      });
    },

    // compute diff since snapshot, recursive,  internal use
    _diff : function() {
      if (this.isNew()) {       // new, return all, create
        return {
          _op: '+', _data: _.clone(this.attributes)
        };
      } else if (!this._snapshotAttributes) {
        throw new Error("please invoke snapshot() first");
      } else {
        var now = this.attributes,
            old = this._snapshotAttributes, changed = {};
        for (var attr in now) {
          var nowVal = now[attr], oldVal = old[attr];
          if(nowVal && _.isFunction(nowVal._diff)) { // a model or collection
            var tmp = nowVal._diff();
            if(tmp) {
              changed[attr] = tmp;
            }
          } else if ( !_.isEqual(oldVal, nowVal)) { // plain data
            changed[attr] = nowVal;
          }
        }
        if(_.isEmpty(changed)) { // no change detected
          return false;         // return false simplify other code
        }

        var where = {}; // server need id to determine which to udpate
        where[this.idAttribute] = this.id;
        return { _op: '!', _data: changed, _where: where};
      }
    }
  });

  var Collection = Backbone.Collection.extend({

    model: Model,               // needs OC Model

    // 1. save a snapshot of ids to detect any remove,
    // 2. all model recursivly do snapshot
    snapshot : function() {
      if(this.length > 0) {
        var idAttribute =  this._getModelIDAttribute();
        this._snapshotModels = _.clone(this.models); // save all models
        this.map(function(model) {      // all model recusive do it
          model.snapshot();
        });
      }
    },

    // get model's idAttrubute
    _getModelIDAttribute : function() {
      return this.model.prototype.idAttribute;
    },

    _diff : function() {
      var that = this,
          removed = _.filter(this._snapshotModels, function(model) {
            return !that.include(model);
          }),
          modelsDiff = _.compact(this.map(function (model) {
            return model._diff();
          }));

      // changed = removed + modified + newly
      var changed = modelsDiff.concat(_.map(removed, function(model) {
        var where = { };
        where[model.idAttribute] = model.id;
        return { _op: '-',_where: where}; // delete
      }));

      if(changed.length === 0) changed = false;
      return changed;
    }
  });

  // a wrapper of backbone's sync to support diffs
  var sync = function(method, model, options) {
    notif.msg(loading);
    if(method !== 'sync') {
      return handler(backboneSync(method, model, options)); // default
    } else {
      var params = _.extend({     // Default JSON-request options.
        type        : 'POST',
        dataType    : 'json',
        processData : false
      }, options);

      if (!params.url) {    // Ensure that we have a URL.
        params.url = getUrl(model);
      }

      // Ensure that we have the appropriate request data.
      if (!params.data && model instanceof Model) {
        params.contentType = 'application/json';
        var diff = model._diff();
        if (!diff) {
          throw new Error("no diff find");
        }
        params.data = JSON.stringify(diff);
      }
      // Backbone.Sync support emulateHTTP & emulateJSON.
      return handler($.ajax(params)); // return a jQuery defered
    }
  };

  // String, Numbers, Boleans are not object, object literal are object
  var isObject = function(obj) {
    return obj && obj.constructor === Object;
  };

  // Copyed from Backbone
  var getUrl = function(object) {
    if (!(object && object.url)) {
      throw new Error('A "url" property or function must be specified');
    };
    return _.isFunction(object.url) ? object.url() : object.url;
  };


  // Wrap an optional error callback with a fallback error event.
  var wrapError = function(onError, model, options) {
    return function(resp) {
      if (onError) {
        onError(model, resp, options);
      } else {
        model.trigger('error', model, resp, options);
      }
    };
  };

  root.OC_Backbone = $.extend(root.OC_Backbone, {   // export
    Model      : Model,
    Collection : Collection,
    sync       : sync           // export
  });
  Backbone.sync = sync;         // wrap backbone sync
})();
