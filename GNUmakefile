##############################################################################
#
# Copyright (C) Zenoss, Inc. 2013, all rights reserved.
#
# This content is made available according to terms specified in
# License.zenoss under the directory where your Zenoss product is installed.
#
##############################################################################


PROJECT=$(PWD)

PROJECT_NAME=central-query

JAVADIR=$(PROJECT)

TARGETDIR=$(JAVADIR)/target

INSTALL_DIR ?= $(PROJECT)/install

SUPERVISOR_CONF = $(PROJECT_NAME)_supervisor.conf

SUPERVISORD_DIR = $(INSTALL_DIR)/etc/supervisor

default: build

clean:
	cd $(JAVADIR) && mvn clean

build-java:
	cd $(JAVADIR) && mvn package

build: build-java

.PHONY: install assemble

ARTIFACT_TAR=$(TARGETDIR)/assembly.tar.gz

$(ARTIFACT_TAR):
	cd $(JAVADIR) && mvn package -P assemble
	cd $(TARGETDIR) && ln -sf *tar.gz assembly.tar.gz

assemble: $(ARTIFACT_TAR)
	echo "debug $(@)"

install:$(ARTIFACT_TAR)
	echo "Installing $(PROJECT_NAME) into $(INSTALL_DIR)"
	mkdir -p $(INSTALL_DIR)
	cd $(INSTALL_DIR) && tar -xvf $(TARGETDIR)/assembly.tar.gz
	mkdir -p $(SUPERVISORD_DIR)
	ln -sf ../$(PROJECT_NAME)/$(SUPERVISOR_CONF) $(SUPERVISORD_DIR)/$(SUPERVISOR_CONF)
	mkdir -p $(INSTALL_DIR)/log
