import os
import json
import xml.dom.minidom
from xml.dom.minidom import Node

def load_file(filename):
  with open(filename, 'r') as f:
    return f.read()

def write_to_file(filename, content):
  with open(filename, 'w') as f:
    f.write(content)

def make_dir(dirname):
  os.makedirs(dirname, exist_ok=True)

def strip_xml_whitespace(xml_string):
  dom = xml.dom.minidom.parseString(xml_string)
  remove_blanks(dom)
  dom.normalize()
  return dom.toxml()

def remove_blanks(node):
  for x in node.childNodes:
    if x.nodeType == Node.TEXT_NODE:
      if x.nodeValue:
        x.nodeValue = x.nodeValue.strip()
    elif x.nodeType == Node.ELEMENT_NODE:
        remove_blanks(x)

os.chdir('samples/')
core_message_id_to_repack = os.environ['TARGET_MESSAGE_ID']

id = core_message_id_to_repack
mhs_json_out_dir = 'mhs-json-out'
make_dir(mhs_json_out_dir)
target_filename = mhs_json_out_dir + '/' + id



ebXml = strip_xml_whitespace(load_file(id + '/ebxml.xml'))
payload = strip_xml_whitespace(load_file(id + '/payload.xml'))

mhs_json_data = {
  'ebXML': ebXml,
  'payload': payload,
  'attachments': []
}

write_to_file(target_filename, json.dumps(mhs_json_data))


