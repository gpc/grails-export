package grails.plugins.export.exporter

import groovy.util.logging.Slf4j
import org.apache.commons.beanutils.PropertyUtils

/**
 * @author Andreas Schmitt
 * 
 */
@Slf4j
class ExporterUtil {
	
	private static Map excludes = [hasMany: true, belongsTo: true, searchable: true, __timeStamp: true, 
	                constraints: true, version: true, metaClass: true, '$callSiteArray': true, 
					'$ownClass': true, '$staticClassInfo': true, '$staticClassInfo$': true, 
					mapping: true, '$defaultDatabindingWhiteList': true, '__$stMC': true,
					errors: true, instanceControllersDomainBindingApi: true, 
					instanceConvertersApi: true, instanceDatabindingApi: true,
					'log': true]
	
	private ExporterUtil(){
		throw new AssertionError()
	}
	
	public static Object getNestedValue(Object domain, String field){
		try {
			// Doesn't work for dynamic properties such as tags used in taggable
			return PropertyUtils.getProperty(domain, field)
		}
		catch(Exception ex){
			// Alternative method for dynamic properties such as tags used in taggable			
			def subProps = field.split("\\.")
			
			int i = 0
			def lastProp
			for(prop in subProps){
				if(i == 0){
					try {
						lastProp = domain?."$prop"
					}
					catch(Exception e){
						log.info("Couldn't retrieve property ${prop}", e)
					}
				}
				else {
					try {
						lastProp = lastProp?."$prop"
					}
					catch(Exception e){
						log.info("Couldn't retrieve property ${prop}", e)
					}
				}
				i += 1
			}
			
			return lastProp
		}
	}
	
	public static List getFields(Object domain){
		List props = []
		
		if(domain instanceof Map){
			domain?.each { key, value ->
				props.add(key)
			}
		}
		else if(domain instanceof Class){
			domain?.properties?.declaredFields.each { field ->
				if(!excludes.containsKey(field.name) && !field.name.contains("class\$") && !field.name.startsWith("__timeStamp") && field.name != "id"){
					props.add(field.name)
				}
			}			
		}
		else {
			domain?.class?.properties?.declaredFields.each { field ->
				if(!excludes.containsKey(field.name) && !field.name.contains("class\$") && !field.name.startsWith("__timeStamp")){
					props.add(field.name)
				}
			}
		}
		
		// Get super class attributes
		Class superClazz = null
		
		if(domain instanceof Map){
			superClazz = null
		}
		else if(domain instanceof Class){
			superClazz = domain?.getSuperclass()
		}
		else {
			superClazz = domain?.class.getSuperclass()
		}
		
		if(superClazz){
			props.addAll(getFields(superClazz))
		}
		
		props.sort()
		
		return props
	}
	
}