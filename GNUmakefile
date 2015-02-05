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
COMPONENT             = central-query-app
SUPERVISOR_CONF       = $(_COMPONENT)_supervisor.conf
COMPONENT_SH          = $(_COMPONENT).sh
SUPERVISORD_DIR       = $(pkgconfdir)/supervisor
REQUIRES_JDK          = 1
#srcdir                = src
#
# For zapp components, keep blddir aligned with src/main/assembly/zapp.xml
#
blddir                = target

#============================================================================
# Hide common build macros, idioms, and default rules in a separate file.
#============================================================================

#---------------------------------------------------------------------------#
# Pull in zenmagic.mk
#---------------------------------------------------------------------------#
# Locate and include common build idioms tucked away in 'zenmagic.mk'
# This holds convenience macros and default target implementations.
#
# Generate a list of directories starting here and going up the tree where we
# should look for an instance of zenmagic.mk to include.
#
#     ./zenmagic.mk ../zenmagic.mk ../../zenmagic.mk ../../../zenmagic.mk
#---------------------------------------------------------------------------#
NEAREST_ZENMAGIC_MK := $(word 1,$(wildcard ./zenmagic.mk $(shell for slash in $$(echo $(abspath .) | sed -e "s|.*\(/obj/\)\(.*\)|\1\2|g" -e "s|.*\(/src/\)\(.*\)|\1\2|g" | sed -e "s|[^/]||g" -e "s|/|/ |g"); do string=$${string}../;echo $${string}zenmagic.mk; done | xargs echo)))

ifeq "$(NEAREST_ZENMAGIC_MK)" ""
    $(warning "Missing zenmagic.mk needed by the $(COMPONENT)-component makefile.")
    $(warning "Unable to find our file of build idioms in the current or parent directories.")
    $(error   "A fully populated src tree usually resolves that.")
else
    #ifneq "$(MAKECMDGOALS)" ""
    #    $(warning "Including $(NEAREST_ZENMAGIC_MK) $(MAKECMDGOALS)")
    #endif
    include $(NEAREST_ZENMAGIC_MK)
endif

# Name of jar we're building: my-component-x.y.z.jar
COMPONENT_SRC ?= $(DFLT_COMPONENT_SRC)
COMPONENT_VERSION ?= $(DFLT_COMPONENT_VERSION)
COMPONENT_JAR ?= $(DFLT_COMPONENT_JAR)

# Specify install-related directories to create as part of the install target.
INSTALL_MKDIRS = $(_DESTDIR)$(prefix) $(_DESTDIR)$(prefix)/log $(_DESTDIR)$(SUPERVISORD_DIR)

ifeq "$(COMPONENT_JAR)" ""
    $(call echol,"Please investigate the COMPONENT_JAR macro assignment.")
    $(error Unable to derive component jar filename from pom.xml)
else
    # Name of binary tar we're building: my-component-x.y.z-zapp.tar.gz
    COMPONENT_TAR = $(shell echo $(COMPONENT_JAR) | $(SED) -e "s|\.jar|-zapp.tar.gz|g")
endif
TARGET_JAR := $(COMPONENT)/$(blddir)/$(COMPONENT_JAR)
TARGET_TAR := $(COMPONENT)/$(blddir)/$(COMPONENT_TAR)

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
	$(call cmd,UNTAR,$(abspath $(TARGET_TAR)),$(_DESTDIR)$(prefix))
	$(call cmd,SYMLINK,../$(_COMPONENT)/$(SUPERVISOR_CONF),$(_DESTDIR)$(SUPERVISORD_DIR)/$(SUPERVISOR_CONF))
	@$(call echol,$(LINE))
	$(call cmd,CHMOD,744,$(_DESTDIR)$(bindir)/$(COMPONENT_SH))
	@$(call echol,$(LINE))
	@$(call echol,"$(_COMPONENT) installed to $(_DESTDIR)$(prefix)")

devinstall: dev% : %
	@$(call echol,"Add logic to the $@ rule if you want it to behave differently than the $< rule.")

uninstall: dflt_component_uninstall

clean: dflt_component_clean

mrclean distclean: dflt_component_distclean

help: dflt_component_help
