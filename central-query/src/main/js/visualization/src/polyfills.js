/**
 * polyfills.js
 * Polyfill any required javascript features that are missing
 */
(function(){
    // make sure that Array.forEach is available
    if (!('forEach' in Array.prototype)) {
        Array.prototype.forEach= function(action, that /*opt*/) {
            for (var i= 0, n= this.length; i<n; i++) {
                if (i in this) {
                    action.call(that, this[i], i, this);
                }
            }
        };
    }

    if (typeof String.prototype.endsWith !== 'function') {
        String.prototype.endsWith = function(suffix) {
            return this.indexOf(suffix, this.length - suffix.length) !== -1;
        };
    }

    if (typeof String.prototype.startsWith !== 'function') {
        String.prototype.startsWith = function(prefix) {
            return this.indexOf(prefix) === 0;
        };
    }


    // for phantomjs and running graph reports
    if (!Function.prototype.bind) {
        Function.prototype.bind = function(oThis) {
            if (typeof this !== 'function') {
                // closest thing possible to the ECMAScript 5
                // internal IsCallable function
                throw new TypeError('Function.prototype.bind - what is trying to be bound is not callable');
            }

            var aArgs   = Array.prototype.slice.call(arguments, 1),
            fToBind = this,
            fNOP    = function() {},
            fBound  = function() {
                    return fToBind.apply(this instanceof fNOP
                                         ? this
                                         : oThis,
                                         aArgs.concat(Array.prototype.slice.call(arguments)));
            };

            if (this.prototype) {
                // native functions don't have a prototype
                fNOP.prototype = this.prototype;
            }
            fBound.prototype = new fNOP();

            return fBound;
        };
    }
})();
