/* tslint:disable */
/* eslint-disable */
/**
 * OpenAPI Petstore
 * This spec is mainly for testing Petstore server and contains fake endpoints, models. Please do not use this for any other purpose. Special characters: \" \\
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { mapValues } from '../runtime';
/**
 * Model for testing model name starting with number
 * @export
 * @interface Model200Response
 */
export interface Model200Response {
    /**
     * 
     * @type {number}
     * @memberof Model200Response
     */
    name?: number;
    /**
     * 
     * @type {string}
     * @memberof Model200Response
     */
    _class?: string;
}

/**
 * Check if a given object implements the Model200Response interface.
 */
export function instanceOfModel200Response(value: object): value is Model200Response {
    return true;
}

export function Model200ResponseFromJSON(json: any): Model200Response {
    return Model200ResponseFromJSONTyped(json, false);
}

export function Model200ResponseFromJSONTyped(json: any, ignoreDiscriminator: boolean): Model200Response {
    if (json == null) {
        return json;
    }
    return {
        
        'name': json['name'] == null ? undefined : json['name'],
        '_class': json['class'] == null ? undefined : json['class'],
    };
}

export function Model200ResponseToJSON(json: any): Model200Response {
    return Model200ResponseToJSONTyped(json, false);
}

export function Model200ResponseToJSONTyped(value?: Model200Response | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'name': value['name'],
        'class': value['_class'],
    };
}

