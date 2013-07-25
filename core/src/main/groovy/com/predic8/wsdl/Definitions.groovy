/* Copyright 2012 predic8 GmbH, www.predic8.com
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License. */

package com.predic8.wsdl

import groovy.xml.MarkupBuilder
import groovy.xml.QName as GQName

import javax.xml.namespace.QName as JQName

import sun.org.mozilla.classfile.internal.SuperBlock;

import com.predic8.policy.*
import com.predic8.schema.*
import com.predic8.soamodel.Consts
import com.predic8.wsdl.creator.WSDLCreator
import com.predic8.wsdl.creator.WSDLCreatorContext
import com.predic8.wsdl.http.HTTPBinding
import com.predic8.wsdl.soap11.SOAPBinding as SOAP11Binding
import com.predic8.wsdl.soap12.SOAPBinding as SOAP12Binding
import com.predic8.xml.util.*


class Definitions extends WSDLElement{

	public static final JQName ELEMENTNAME = new JQName(Consts.WSDL11_NS, 'definitions')

	def resourceResolver

	def baseDir
	String targetNamespace = ''

	/**
	 * The local-prefix indicates that the elements are defined in this WSDL document. 
	 * Calling e.g. getBindings will return all the bindings in an wsdl:import hierarchy
	 */
	Types localTypes 
	List<Message> localMessages = []
	List<PortType> localPortTypes = []
	List<Binding> localBindings = []
	List<Service> localServices = []

	List<Import> imports = []

	/**
	 * If there are imported WSDLs, the complete information about the WSDL elements 
	 * should be collected from the WSDLs in the registry.
	 */
	Registry registry = new Registry() 

	public Definitions() {
		registry.add(this)
	}

	public Definitions(String tns, String name) {
		this.name = name
		this.namespaces[''] = 'http://schemas.xmlsoap.org/wsdl/'
		this.namespaces['wsdl'] = 'http://schemas.xmlsoap.org/wsdl/'
		this.namespaces['tns'] = tns
		this.targetNamespace = tns
		registry.add(this)
	}

	/**
	 * All the getter methods return the local elements and elements defined in other 
	 * WSDLs with the same namespace. 
	 */
	public List<Types> getTypes() {
		registry.getWsdls(targetNamespace)*.localTypes.flatten()
	}

	public List<Message> getMessages() {
		allWSDLs.localMessages.flatten()
	}

	public List<PortType> getPortTypes() {
		allWSDLs.localPortTypes.flatten()
	}

	public List<Binding> getBindings() {
		allWSDLs.localBindings.flatten()
	}

	public List<Service> getServices() {
		allWSDLs.localServices.flatten()
	}

	String getTargetNamespacePrefix() {
		getPrefix(targetNamespace)
	}
	
	public void setTypes(Types types){
		localTypes = types
	}
	
	Boolean isConcrete() {
		getBindings("SOAP11")[0]
	}

	PortType getPortType(String name) {
		portTypes.find{ it.name == name }
	}

	PortType getPortType(GQName qname) {
		lookup("portTypes", qname)
	}

	List<Operation> getOperations() {
		(portTypes.operations).flatten().unique()
	}

	Operation getOperation(String name, String portType) {
		getPortType(portType).operations.find{ name == it.name }
	}

	Operation getOperation(String name, GQName portType) {
		getPortType(portType).operations.find{ name == it.name }
	}

	Message getMessage(String name) {
		messages.find { it.name == name }
	}

	Message getMessage(GQName qname) {
		lookup("messages", qname)
	}
	
	Binding getBinding(GQName qname) {
		lookup("bindings", qname)
	}

	def lookup = { item, qname -> registry.getWsdls(qname.namespaceURI)*."$item".flatten().find{it.name == qname.localPart}}
	
	Element getElement(String name) {
		getElement(getQNameForPN(new PrefixedName(name)))
	}

	Element getElement(GQName qname) {
		schemas.elements.flatten().find{
			it.schema.targetNamespace == qname.namespaceURI && it.name == qname.localPart
		}
	}
	
	TypeDefinition getSchemaType(String name) {
		getSchemaType(getQNameForPN(new PrefixedName(name)))
	}
	
	TypeDefinition getSchemaType(GQName qname) {
		if(qname?.namespaceURI == Consts.SCHEMA_NS) return new BuiltInSchemaType(qname: qname)
		schemas.find{ it.getType(qname) }?.getType(qname) 
	}

	Element getElementForOperation(String operation, portType){
		getOperation(operation,portType).input.message.parts.flatten()[0]?.element
	}

	List<Binding> getBindings(protocol) {
		bindings.findAll{it.protocol == protocol}
	}

	Binding getBinding(String name) {
		bindings.find { it.name == name }
	}

	def getSoap11Binding(name) {
		bindings.binding.find { it instanceof SOAP11Binding && it.name == name }
	}

	def getSoap12Binding(name) {
		bindings.binding.find { it instanceof SOAP12Binding && it.name == name }
	}

	def getHTTPBinding(name) {
		bindings.binding.find { it instanceof HTTPBinding && it.name == name }
	}

	protected parseAttributes(token, ctx){
		targetNamespace = ctx.targetNamespace ?: token.getAttributeValue( null , 'targetNamespace')
		registry.add(this)
		name = token.getAttributeValue( null , 'name')
	}

	protected parseChildren(token, child, ctx){
		super.parseChildren(token, child, ctx)
		switch (token.name) {
			case Policy.ELEMENTNAME :
				def policy = new Policy(wsdlElement: this, parent : parent)
				policy.parse(token, ctx)
					policies << policy ; break
			case Import.ELEMENTNAME :
				def imp = new Import(definitions : this)
				imp.parse(token, ctx)
					imports << imp ; break
			case Types.ELEMENTNAME :
				localTypes = new Types(definitions : this)
					localTypes.parse(token, ctx) ; break
			case Message.ELEMENTNAME :
				def message = new Message(definitions: this)
				message.parse(token, ctx)
					localMessages << message ; break
			case PortType.ELEMENTNAME:
				def portType = new PortType(definitions:this)
				portType.parse(token, ctx)
					localPortTypes << portType ; break
			case Binding.ELEMENTNAME :
				def binding = new Binding(definitions: this)
				binding.parse(token, ctx)
					localBindings << binding; break
			case Service.ELEMENTNAME :
				def service = new Service(definitions : this)
				service.parse(token, ctx)
					localServices << service; break

			default :
				if(token.name != Documentation.ELEMENTNAME && token.name != Policy.ELEMENTNAME )
					ctx.errors << "${token.name} in a wsdl is not supported yet!"
				break
		}
	}

	List<Schema> getLocalSchemas(){
		localTypes?.schemas
	}
	
	List<Schema> getSchemas(){
		types.allSchemas.flatten()
	}

	Schema getSchema(String targetNamespace){
		schemas.find{ it.targetNamespace == targetNamespace }
	}

	def getService(GQName qname){
		lookup("services", qname)
	}

	def create(creator, ctx){
		creator.createDefinitions(this, ctx)
	}

	List<Definitions> getAllWSDLs(){
		registry.getWsdls(targetNamespace)
	}

	public void addSchema(Schema schema){
		if(!localTypes) localTypes = new Types()
		localTypes.schemas << schema
	}

	public Message newMessage(String name){
		def msg = new Message(definitions:this, name:name, parent: this)
		localMessages << msg
		msg
	}

	public PortType newPortType(String name){
		def pt = new PortType(definitions:this, name:name, parent: this)
		localPortTypes << pt
		pt
	}

	public Binding newBinding(String name){
		def bnd = new Binding(definitions:this, name:name, parent: this)
		localBindings << bnd
		bnd
	}

	public Service newService(String name){
		def service = new Service(name : name, definitions: this, parent: this)
		localServices << service
		service
	}

	String getAsString(){
		StringWriter writer = new StringWriter()
		create(new WSDLCreator(builder: new MarkupBuilder(writer)), new WSDLCreatorContext())
		writer.toString()
	}

	String toString() {
		"defintions[ baseDir=$baseDir, targetNamespace=$targetNamespace, namespaces=$namespaceContext, services=$localServices, documentation=$documentation, schemas=$localSchemas, bindings=$localBindings, messages=$localMessages, portTypes=$localPortTypes]"
	}
}
