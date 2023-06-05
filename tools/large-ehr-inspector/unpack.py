import glob
import os
import json
import xml.dom.minidom
import xml.etree.ElementTree as ET

def pretty_xml(xml_string):
  dom = xml.dom.minidom.parseString(xml_string)
  return dom.toprettyxml()


def msgid(mhs_json_filename):
  return os.path.basename(os.path.normpath(mhs_json_filename))


def load_json_file(filename):
  with open(filename, 'r') as json_string:
    return json.load(json_string)


def write_to_file(filename, content):
  with open(filename, 'w') as f:
    f.write(content)


def make_dir(dirname):
  os.makedirs(dirname, exist_ok=True)


def print_xpath(context, root, xpath):
  print(context, 'xpath', xpath)
  print(context, 'results', [ match for match in root.findall(xpath)])


def inspect_ebxml(ebxml):
  root = ET.fromstring(ebxml)
  print_xpath('ebxml', root, ".//{*}Reference")


def inspect_fragments(id, fragments):
  print('message fragments', len(fragments))
  for fragment in fragments:
    print('fragment keys', fragment.keys())
    print('fragment content_id', fragment['content_id'])
    print('fragment content_type', fragment['content_type'])
    print('fragment is base 64?', fragment['is_base64'])
    print('fragment payload length', len(fragment['payload']))
    write_to_file(id + '/' + fragment['content_id'].replace('/', '-'), fragment['payload'])


os.chdir('samples/')
mhs_json_filenames = glob.glob('mhs-json/*')


for filename in mhs_json_filenames:
  data = load_json_file(filename)
  print('keys', data.keys())

  id = msgid(filename)
  make_dir(id)
  print('processing message id', id)

  inspect_ebxml(data['ebXML'])
  inspect_fragments(id, data['attachments'])

  write_to_file(id + '/ebxml.xml', pretty_xml(data['ebXML']))
  write_to_file(id + '/payload.xml', pretty_xml(data['payload']))

