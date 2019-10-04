#!/usr/bin/env python3

import argparse

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--offset', type=str, default='0x1000')
    parser.add_argument('--input', type=str, required=True)
    parser.add_argument('--output', type=str, required=True)
    args = parser.parse_args()

    print(args.input)

    with open(args.input, mode='rb') as f:
        bin_data = f.read()

    #print(bin_data)
    #print(bin_data[0])
    hex_data = [int.from_bytes(bin_data[i:i+4], 'little') for i in range(0, len(bin_data), 4)]

    with open(args.output, mode='w') as f:
        f.write('@{:08x}\n'.format(int(args.offset, 16) >> 2))

        for i in hex_data:
            f.write('{:08x}\n'.format(i))


if __name__ == '__main__':
    main()
