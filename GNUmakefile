#============================================================================
#
# Copyright (C) Zenoss, Inc. 2013, all rights reserved.
#
# This content is made available according to terms specified in
# License.zenoss under the directory where your Zenoss product is installed.
#
#============================================================================
.DEFAULT_GOAL   := help # all|build|clean|distclean|devinstall|install|help

#============================================================================
# Build component configuration.
#
# Beware of trailing spaces.
# Don't let your editor turn tabs into spaces or vice versa.
#============================================================================
COMPONENT             = central-query
COMPONENT_PREFIX      = install
COMPONENT_SYSCONFDIR  = $(COMPONENT_PREFIX)/etc
_COMPONENT            = $(strip $(COMPONENT))
SUPERVISOR_CONF       = $(_COMPONENT)_supervisor.conf
SUPERVISORD_DIR       = $(SYSCONFDIR)/supervisor
REQUIRES_JDK          = 1
#COMPONENT_MAVEN_OPTS = -DskipTests=true
SRC_DIR               = src
#
# For zapp components, keep BUILD_DIR aligned with src/main/assembly/zapp.xml
#
BUILD_DIR             = target

#============================================================================
# Hide common build macros, idioms, and default rules in a separate file.
#============================================================================
ifeq "$(wildcard zenmagic.mk)" ""
    $(error "Makefile for $(_COMPONENT) is unable to include zenmagic.mk.  Please investigate")
else
    include zenmagic.mk
endif

# List of source files needed to build this component.
COMPONENT_SRC ?= $(DFLT_COMPONENT_SRC)

# Name of jar we're building: my-component-x.y.z.jar
COMPONENT_JAR ?= $(DFLT_COMPONENT_JAR)

# Specify install-related directories to create as part of the install target.
# NB: Intentional usage of _PREFIX and PREFIX here to avoid circular dependency.
INSTALL_MKDIRS = $(_DESTDIR)$(_PREFIX) $(_DESTDIR)$(PREFIX)/log $(_DESTDIR)$(SUPERVISORD_DIR)

ifeq "$(COMPONENT_JAR)" ""
    $(call echol,"Please investigate the COMPONENT_JAR macro assignment.")
    $(error Unable to derive component jar filename from pom.xml)
else
    # Name of binary tar we're building: my-component-x.y.z-zapp.tar.gz
    COMPONENT_TAR = $(shell echo $(COMPONENT_JAR) | $(SED) -e "s|\.jar|-zapp.tar.gz|g")
endif
TARGET_JAR := $(BUILD_DIR)/$(COMPONENT_JAR)
TARGET_TAR := $(BUILD_DIR)/$(COMPONENT_TAR)

#============================================================================
# Subset of standard build targets our makefiles should implement.  
#
# See: http://www.gnu.org/prep/standards/html_node/Standard-Targets.html#Standard-Targets
#============================================================================
.PHONY: all build clean devinstall distclean install help mrclean uninstall
all build: $(TARGET_TAR)

# Targets to build the binary *.tar.gz.
ifeq "$(_TRUST_MVN_REBUILD)" "yes"
$(TARGET_TAR): checkenv
else
$(TARGET_TAR): $(CHECKED_ENV) $(COMPONENT_SRC)
endif
	$(call cmd,MVNASM,package -P assemble,$@)
	@$(call echol,$(LINE))
	@$(call echol,"$(_COMPONENT) built.  See $@")

$(INSTALL_MKDIRS):
	$(call cmd,MKDIR,$@)

# NB: Use the "|" to indicate an existence-only dep rather than a modtime dep.
#     This rule should not trigger rebuilding of the component we're installing.
install: | $(INSTALL_MKDIRS) 
	@if [ ! -f "$(TARGET_TAR)" ];then \
		$(call echol) ;\
		$(call echol,"Error: Missing $(TARGET_TAR)") ;\
		$(call echol,"Unable to $@ $(_COMPONENT).") ;\
		$(call echol,"$(LINE)") ;\
		$(call echol,"Please run 'make build $@'") ;\
		$(call echol,"$(LINE)") ;\
		exit 1 ;\
	fi 
	$(call cmd,UNTAR,$(abspath $(TARGET_TAR)),$(_DESTDIR)$(PREFIX))
	$(call cmd,SYMLINK,../$(_COMPONENT)/$(SUPERVISOR_CONF),$(_DESTDIR)$(SUPERVISORD_DIR)/$(SUPERVISOR_CONF))
	@$(call echol,$(LINE))
	@$(call echol,"$(_COMPONENT) installed to $(_DESTDIR)$(PREFIX)")

devinstall: dev% : %
	@$(call echol,"Add logic to the $@ rule if you want it to behave differently than the $< rule.")

uninstall: dflt_component_uninstall

clean: dflt_component_clean

mrclean distclean: dflt_component_distclean

help: dflt_component_help
