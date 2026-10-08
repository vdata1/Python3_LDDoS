#!/usr/bin/env python3

import argparse
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.dates as mdates


def main():
    parser = argparse.ArgumentParser(
        description="Plot two columns from a CSV file as a line plot."
    )

    parser.add_argument(
        "-f",
        "--file",
        required=True,
        help="CSV file name"
    )

    parser.add_argument(
        "-x",
        "--x",
        required=True,
        help="Column to use for the X axis"
    )

    parser.add_argument(
        "-y",
        "--y",
        required=True,
        help="Column to use for the Y axis"
    )

    args = parser.parse_args()

    # Read CSV
    try:
        data = pd.read_csv(args.file)
    except FileNotFoundError:
        print(f"Error: File not found: {args.file}")
        return
    except Exception as e:
        print(f"Error reading CSV file: {e}")
        return

    # Check columns
    if args.x not in data.columns:
        print(f"Error: X column '{args.x}' not found.")
        print("Available columns:")
        for column in data.columns:
            print(f"  - {column}")
        return

    if args.y not in data.columns:
        print(f"Error: Y column '{args.y}' not found.")
        print("Available columns:")
        for column in data.columns:
            print(f"  - {column}")
        return

    # Try to detect/parse datetime values in X
    x_datetime = pd.to_datetime(data[args.x], errors="coerce")

    # If most values can be interpreted as dates, use datetime
    valid_datetime_ratio = x_datetime.notna().mean()

    if valid_datetime_ratio >= 0.8:
        data[args.x] = x_datetime
        is_datetime = True
    else:
        is_datetime = False

    # Convert Y values to numeric
    data[args.y] = pd.to_numeric(data[args.y], errors="coerce")

    # Remove invalid rows
    data = data.dropna(subset=[args.x, args.y])

    if data.empty:
        print("Error: No valid data points found.")
        return

    # Sort by X axis
    data = data.sort_values(by=args.x)

    # Create plot
    plt.figure(figsize=(12, 6))

    plt.plot(
        data[args.x],
        data[args.y],
        marker="o",
        markersize=3,
        linewidth=1.5
    )

    plt.xlabel(args.x)
    plt.ylabel(args.y)
    plt.title(f"{args.y} vs {args.x}")

    # Format datetime X-axis
    if is_datetime:
        ax = plt.gca()

        # Automatically choose a sensible date format
        locator = mdates.AutoDateLocator()
        formatter = mdates.ConciseDateFormatter(locator)

        ax.xaxis.set_major_locator(locator)
        ax.xaxis.set_major_formatter(formatter)

    plt.grid(True, alpha=0.3)
    plt.tight_layout()

    plt.show()


if __name__ == "__main__":
    main()
