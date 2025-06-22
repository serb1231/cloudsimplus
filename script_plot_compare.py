import pandas as pd
import matplotlib.pyplot as plt
import os

def main():
    # Load CSV from the same directory
    filename = "sla_summary.csv"
    if not os.path.exists(filename):
        print(f"File '{filename}' not found in current directory.")
        return

    df = pd.read_csv(filename)

    # Calculate derived metrics
    df["PowerSaved"] = df.groupby("Algorithm")["PowerConsumption"].transform(
        lambda x: (x.max() - x) / x.max() * 100
    )
    df["SLA_SuccessPct"] = 100 - df["ViolationPct"]

    # Plot SLA Success vs Power Saved
    plt.figure(figsize=(10, 6))
    for name, group in df.groupby("Algorithm"):
        plt.plot(group["PowerSaved"], group["SLA_SuccessPct"], marker='o', label=name)

    # Labeling
    plt.xlabel("Power Saved (%)")
    plt.ylabel("Jobs Finished On Time (%)")
    plt.title("SLA Success vs Power Saved per Algorithm")
    plt.legend(title="Algorithm")
    plt.grid(True)
    plt.tight_layout()

    # Show the plot
    plt.show()

if __name__ == "__main__":
    main()
