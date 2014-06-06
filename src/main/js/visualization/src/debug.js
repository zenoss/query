/**
 * debug.js
 * debug/logging utils
 */
(function(){
    "use strict";

    debug = {
        
        /**
         * Used to enable (true) or disable (false) debug output to the
         * browser console
         *
         * @access public
         * @default false
         */
        debug: false,
        
        /**
         * Wrapper around the console group function. This wrapper protects
         * the client from those browsers that don't support the group
         * function.
         *
         * @access private
         */
        __group: function() {
            if (console !== undefined) {
                if (console.group !== undefined) {
                    console.group.apply(console, arguments);
                } else if (console.log !== undefined) {
                    console.log.apply(console, arguments);
                }
                // Oh well
            }
        },

        /**
         * Wrapper around the console groupCollapsed function. This wrapper
         * protects the client from those browsers that don't support this
         * function.
         *
         * @access private
         */
        __groupCollapsed: function() {
            if (console !== undefined) {
                if (console.groupCollapsed !== undefined) {
                    console.groupCollapsed.apply(console, arguments);
                } else if (console.log !== undefined) {
                    console.log.apply(console, arguments);
                }
                // Oh well
            }
        },

        /**
         * Wrapper around the console function. This wrapper protects the
         * client from those browsers that don't support this function.
         *
         * @access private
         */
        __groupEnd: function() {
            if (console !== undefined) {
                if (console.groupEnd !== undefined) {
                    console.groupEnd.apply(console, arguments);
                } else if (console.log !== undefined) {
                    console.log.apply(console, [ "END" ]);
                }
                // Oh well
            }
        },

        /**
         * Wrapper around the console function. This wrapper protects the
         * client from those browsers that don't support this function.
         *
         * @access private
         */
        __error: function() {
            if (console !== undefined) {
                if (console.error !== undefined) {
                    console.error(arguments[0]);
                } else if (console.log !== undefined) {
                    console.log.apply(console, arguments);
                }
                // If neither of those exists, oh well ....
            }
        },

        /**
         * Wrapper around the console function. This wrapper protects the
         * client from those browsers that don't support this function.
         *
         * @access private
         */
        __warn: function() {
            if (console !== undefined) {
                if (console.warn !== undefined) {
                    console.warn(arguments[0]);
                } else if (console.log !== undefined) {
                    console.log.apply(console, arguments);
                }
                // If neither of those exists, oh well ....
            }
        },

        /**
         * Wrapper around the console function. This wrapper protects the
         * client from those browsers that don't support this function.
         *
         * @access private
         */
        __log: function() {
            if (console !== undefined) {
                if (console.log !== undefined) {
                    console.log.apply(console, arguments);
                }
                // Oh well
            }
        }
    };
})();