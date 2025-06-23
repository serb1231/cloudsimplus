# import pandas as pd
# import matplotlib.pyplot as plt
# from pathlib import Path

# # Load all CSV files
# input_dir = Path("results_csv/results_multiple_iterations_algorithm")
# csv_files = list(input_dir.glob("*.csv"))

# algo_avg_times = {}

# for file in csv_files:
#     df = pd.read_csv(file)

#     if df.empty:
#         continue

#     df["RunId"] = df["Algorithm"].str.extract(r"times_(\d+)")
#     df["BaseAlgorithm"] = df["Algorithm"].str.extract(r"(^[^_]+)")

#     # Păstrăm doar iterațiile unde SLA < 10%
#     filtered = df[df["ViolationPct"] < 10]
#     grouped = filtered.groupby("RunId")["TotalTimeTaken"].first()  # Fără sumă, doar o valoare per iterație

#     if not grouped.empty:
#         base_name = df["BaseAlgorithm"].iloc[0]
#         algo_avg_times[base_name] = grouped.mean()

# # Convert to DataFrame
# avg_df = pd.DataFrame(list(algo_avg_times.items()), columns=["Algorithm", "AvgTimePerIteration"])

# # Plot
# plt.figure(figsize=(10, 6))
# bars = plt.bar(avg_df["Algorithm"], avg_df["AvgTimePerIteration"], color="skyblue")

# # Add labels in scientific notation
# for bar in bars:
#     height = bar.get_height()
#     plt.text(
#         bar.get_x() + bar.get_width() / 2,
#         height * 1.05,
#         f"{height:.2e}",
#         ha='center',
#         va='bottom',
#         fontsize=10
#     )

# plt.yscale("log")
# plt.ylabel("Average Time per SLA-Compliant Iteration (s)")
# plt.title("Average Compute Time per Algorithm (SLA < 10%)")
# plt.grid(axis='y', which='major', linestyle='--', linewidth=0.7)
# plt.tight_layout()

# # Save plot
# output_dir = Path("plots/scatter_summary")
# output_dir.mkdir(parents=True, exist_ok=True)
# plot_path = output_dir / "average_compute_time_per_algorithm_log.png"
# plt.savefig(plot_path)
# plt.close()


import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path

# Load all CSV files
input_dir = Path("results_csv/results_multiple_iterations_algorithm_smaller_aco")
csv_files = list(input_dir.glob("*.csv"))

algo_avg_times = {}

for file in csv_files:
    df = pd.read_csv(file)

    if df.empty:
        continue

    df["RunId"] = df["Algorithm"].str.extract(r"times_(\d+)")
    df["BaseAlgorithm"] = df["Algorithm"].str.extract(r"(^[^_]+)")

    # Păstrăm doar iterațiile unde SLA < 10%
    filtered = df[df["ViolationPct"] < 10]

    grouped = filtered.groupby("RunId")["TotalTimeTaken"].first() / filtered.groupby("RunId").size()    # Fără sumă, doar o valoare per iterație

    if not grouped.empty:
        base_name = df["BaseAlgorithm"].iloc[0]
        algo_avg_times[base_name] = grouped.mean()

# Convert to DataFrame
avg_df = pd.DataFrame(list(algo_avg_times.items()), columns=["Algorithm", "AvgTimePerIteration"])

# Plot
plt.figure(figsize=(10, 6))
bars = plt.bar(avg_df["Algorithm"], avg_df["AvgTimePerIteration"], color="skyblue")

# Add labels in scientific notation
for bar in bars:
    height = bar.get_height()
    plt.text(
        bar.get_x() + bar.get_width() / 2,
        height * 1.05,
        f"{height:.2e}",
        ha='center',
        va='bottom',
        fontsize=10
    )

plt.yscale("log")
plt.ylabel("Average Time per SLA-Compliant Iteration (s)")
plt.title("Average Compute Time per Algorithm (SLA < 10%)")
plt.grid(axis='y', which='major', linestyle='--', linewidth=0.7)
plt.tight_layout()

# Save plot
output_dir = Path("plots/scatter_summary")
output_dir.mkdir(parents=True, exist_ok=True)
plot_path = output_dir / "average_power_per_run_sla_10_smaller_aco.png"
plt.savefig(plot_path)
plt.close()